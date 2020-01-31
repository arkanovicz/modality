# syntrax spec file for getting a model
choice(
    line('ModelRepository.getModel', '(', opt('/servletContext'), '/"modelId")'),
    line('WebappModelAccessor.getModel()'),
    line('ModelConfig.getModel()')
)
