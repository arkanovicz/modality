# model configuration and initialization

stack(
    line('new Model(', opt('"modelId"'), ');'),
    choice(
        'model.setDatabaseURL("jdbc://...")',
        line('model.setDatasource', '(', choice('/DataSource object', '/"JNDI name"'), ')')
    ),
    loop(
        choice(
            None,
            '.setDefinition(url)',
            '.configure(Map)',
            line('.setSchema', '(', '/"schema"', ')'),
            line('.setReverseMode', '(', choice('/"none"', '/"columns"', '/"tables"', '/"joins"', '/"extended"'), ')'),
            line('.getCredentials()', '.', '/credentials setter'),
            line('.getFilters()', '.', '/filters setter'),
            '/other configuration setter  ...'
        ), None
    ),
    line(
        '.initialize', '(',
        choice(
            None,
            '/definitionURL',
            '/definitionReader',
            '/"definitionPath"'
        ),
        ');'
    )
)
