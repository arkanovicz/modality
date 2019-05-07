package com.republicate.modality.webapp.auth.helpers;

import com.republicate.modality.Model;

import javax.servlet.ServletException;

public interface CredentialsChecker<USER>
{
    default CredentialsChecker<USER> setModel(Model model) throws ServletException
    {
        // nop
        return this;
    }

    USER checkCredentials(String login, String password) throws ServletException;

}
