package com.republicate.modality.config;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.commons.lang3.BooleanUtils;
import org.apache.velocity.tools.ClassUtils;
import org.apache.velocity.util.ExtProperties;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ConfigHelper
{
    public ConfigHelper()
    {
        this(null);
    }

    public ConfigHelper(Map<String, Object> values)
    {
        if (values != null)
        {
            config = ExtProperties.convertProperties(values.entrySet().stream().filter(entry -> entry.getValue() != null).collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue())));
        }
        else
        {
            config = new ExtProperties();
        }
    }

    public ConfigHelper setProperties(ExtProperties values)
    {
        config.combine(values);
        return this;
    }

    public ConfigHelper setProperties(Map values)
    {
        setProperties(ExtProperties.convertProperties(values));
        return this;
    }

    public ConfigHelper setProperties(URL properties)
    {
        ExtProperties props = new ExtProperties();
        try
        {
            props.load(properties.openStream());
        }
        catch (IOException ioe)
        {
            throw new ConfigurationException("coud not load " + properties.toString(), ioe);
        }
        setProperties(props);
        return this;
    }

    private Object getInternal(String key)
    {
        return config.get(key);
    }

    public Object get(String key, Object defaultValue)
    {
        Object value = getInternal(prefix.get() + key);
        return value == null ? defaultValue : value;
    }

    public Object get(String key)
    {
        return get(key, null);
    }

    public String getString(String key, String defaultValue)
    {
        return (String)get(key, defaultValue);
    }

    public String getString(String key)
    {
        return getString(key, null);
    }

    public Boolean getBoolean(String key, Boolean defaultValue)
    {
        Object b = get(key, defaultValue);
        if (b instanceof Boolean)
        {
            return (Boolean)b;
        }
        else if (b instanceof String)
        {
            return BooleanUtils.toBoolean((String)b);
        }
        else
        {
            return defaultValue;
        }
    }

    public Boolean getBoolean(String key)
    {
        return getBoolean(key, null);
    }

    public ExtProperties getSubProperties(String key)
    {
        return config.subset(prefix.get() + key);
    }

    public <T extends Enum<T>> T getEnum(String key, Enum<T> defaultValue) throws IllegalArgumentException
    {
        Object value = get(key, defaultValue);
        if (value == null)
        {
            return null;
        }
        else if (defaultValue.getClass().isAssignableFrom(value.getClass()))
        {
            return (T)value;
        }
        else if (value instanceof String)
        {
            return (T)Enum.valueOf(defaultValue.getClass(), String.valueOf(value).toUpperCase(Locale.ROOT));
        }
        else
        {
            return (T)defaultValue;
        }
    }

    private Boolean webContext = null;
    private Method servletUtils_getURL = null;
    private Method servletContext_getResourcePaths = null;

    private boolean isWebContext(Object servletContext)
    {
        if (webContext != null) return webContext.booleanValue();
        boolean isWebContext = false;
        Class servletContextClass = null;
        Class servletUtilsClass = null;
        try
        {
            servletContextClass = Class.forName("javax.servlet.ServletContext");
            servletUtilsClass = Class.forName("org.apache.velocity.tools.view.ServletUtils");
        }
        catch (ClassNotFoundException cnfe)
        {
        }
        if (servletContextClass != null && servletUtilsClass != null)
        {
            if (servletContext == null)
            {
                servletContext = getInternal("servletContext");
            }
            if (servletContext != null && servletContextClass.isAssignableFrom(servletContext.getClass()))
            {
                // remember it
                config.setProperty("servletContext", servletContext);
                try
                {
                    servletUtils_getURL = servletUtilsClass.getMethod("getURL", String.class, servletContextClass);
                    servletContext_getResourcePaths = servletContextClass.getMethod("getResourcePaths", String.class);
                    isWebContext = true;
                }
                catch (NoSuchMethodException e)
                {
                }
            }
        }
        webContext = Boolean.valueOf(isWebContext);
        return isWebContext;
    }

    public URL findURL(String path)
    {
        return findURL(path, null, true);
    }

    public URL findURL(String path, boolean mandatory)
    {
        return findURL(path, null, mandatory);
    }

    public URL findURL(String path, Object servletContext)
    {
        return findURL(path, servletContext, true);
    }

    public /*static*/ URL findURL(String path, Object servletContext, boolean mandatory)
    {
        URL url = null;
        boolean webContext = isWebContext(servletContext);
        if (webContext)
        {
            try
            {
                // we want an absolute path
                String absolutePath = path;
                if (!absolutePath.startsWith("/")) absolutePath = "/" + absolutePath;
                // only search in WEB-INF/
                if (!absolutePath.startsWith("/WEB-INF/"))
                {
                    absolutePath = "/WEB-INF" + absolutePath;
                }
                if (servletContext == null)
                {
                    servletContext = getInternal("servletContext");
                }
                url = (URL) servletUtils_getURL.invoke(null, new Object[]{absolutePath, servletContext});
            }
            catch (IllegalAccessException | InvocationTargetException e)
            {
                if (mandatory)
                {
                    throw new ConfigurationException("could not get URL for path '" + path + "'", e);
                }
            }
        }
        if (url == null)
        {
            // check class path
            url = ClassUtils.getResource(path, ConfigHelper.class);
            if (url == null && !webContext)
            {
                // check filesystem
                try
                {
                    File defFile = new File(path);
                    if (fileExists(defFile))
                    {
                        url = defFile.toPath().toUri().toURL();
                    }
                    else if (mandatory)
                    {
                        throw new ConfigurationException("cannot find file '" + path + "'");
                    }
                }
                catch (MalformedURLException mfue)
                {
                    throw new ConfigurationException("could not get URL for path '" + path + "'", mfue);
                }
            }
        }
        return url;
    }

    public static final Comparator<URL> urlComparator = (o1, o2) -> o1.toString().compareTo(o2.toString());

    /**
     * Find resources on the file system or in the classpath
     * @param path package name, absolute path or relative path
     * @param pattern filename pattern
     * @return set of resources
     */
    public Set<URL> findResources(String path, String pattern)
    {
        return findResources(path, pattern, null);
    }

    public Set<URL> findResources(String path, String pattern, Object servletContext)
    {
        Set<URL> ret = null;
        Pattern patternRx = Pattern.compile(pattern);
        boolean webContext = isWebContext(servletContext);
        if (webContext)
        {
            if (servletContext == null) servletContext = getInternal("servletContext");
            if (servletContext != null)
            {
                // we want an absolute path
                String absolutePath = path;
                if (!absolutePath.startsWith("/")) absolutePath = "/" + absolutePath;
                // only search in WEB-INF/
                if (!absolutePath.startsWith("/WEB-INF/"))
                {
                    absolutePath = "/WEB-INF" + absolutePath;
                }
                try
                {
                    Set<String> resources = (Set<String>)servletContext_getResourcePaths.invoke(servletContext, absolutePath);
                    if (resources != null)
                    {
                        final Object finalServletContext = servletContext;
                        ret = resources.stream()
                            .filter(resource -> patternRx.matcher(resource).matches())
                            .map(resource -> findURL(resource, finalServletContext, false))
                            .filter(url -> url != null)
                            .collect(Collectors.toCollection(() -> new TreeSet<>(urlComparator)));
                    }
                }
                catch (IllegalAccessException | InvocationTargetException e)
                {
                }
            }
        }
        if (ret == null)
        {
            // check classpath
            String pkg = path.replace('/', '.');
            Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage(pkg))
                .setScanners(Scanners.Resources));
            Set<String> resources = reflections.getResources(patternRx);
            ret = resources.stream()
                .map(resource -> findURL(path, null, false))
                .filter(url -> url != null)
                .collect(Collectors.toCollection(() -> new TreeSet<>(urlComparator)));
            if ((ret == null || ret.isEmpty()) && !webContext)
            {
                // check filesystem
                File defFile = new File(path);
                if (directoryExists(defFile))
                {
                    File[] files = defFile.listFiles(new FilenameFilter()
                    {
                        @Override
                        public boolean accept(File dir, String name)
                        {
                            return patternRx.matcher(name).matches();
                        }
                    });
                    if (files != null)
                    {
                        ret = Arrays.stream(files)
                            .map(file -> {
                                try
                                {
                                    return file.toPath().toUri().toURL();
                                }
                                catch (MalformedURLException mfue)
                                {
                                    throw new ConfigurationException("could not get URL for path '" + file.getAbsolutePath() + "'", mfue);
                                }
                            })
                            .collect(Collectors.toCollection(() -> new TreeSet<>(urlComparator)));
                    }
                }

            }
        }
        return ret;
    }

    protected static boolean fileExists(final File file)
    {
        boolean ret;
        if (System.getSecurityManager() != null)
        {
            ret = AccessController.doPrivileged(
                new PrivilegedAction<Boolean>()
                {
                    @Override
                    public Boolean run()
                    {
                        return file.exists() && file.canRead();
                    }
                });
        }
        else
        {
            ret = file.exists() && file.canRead();
        }
        return ret;
    }

    protected static boolean directoryExists(final File file)
    {
        boolean ret;
        if (System.getSecurityManager() != null)
        {
            ret = AccessController.doPrivileged(
                new PrivilegedAction<Boolean>()
                {
                    @Override
                    public Boolean run()
                    {
                        return file.exists() && file.isDirectory() && file.canRead();
                    }
                });
        }
        else
        {
            ret = file.exists() && file.isDirectory() && file.canRead();
        }
        return ret;
    }


    public void setPrefix(String prefix)
    {
        this.prefix.set(prefix);
    }

    public void resetPrefix()
    {
        this.prefix.set("");
    }

    private ExtProperties config = null;

    private ThreadLocal<String> prefix = ThreadLocal.withInitial(() -> "");
}
