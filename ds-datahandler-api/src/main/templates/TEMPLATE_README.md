# OpenAPI generator templates

Copied and adapted from https://github.com/OpenAPITools/openapi-generator/
to provide streaming support.


# API and Impl, concrete source unknown, adapted for streaming support

 * `api.mustache`
 * `apiServiceImpl.mustache`
 * `returnTypes.mustache`

# Model (DTO), taken from openapi-generator/..Java/ version 4.2.2

This was done to get withXML to add `@JacksonXmlRootElement` and other Jackson annotations
needed for returning XML with proper element names (`book` instead of `BookDto` etc.).

 * pojo.mustache
 * model.mustache (adapted with extra imports)
 * modelInnerEnum.mustache
 * jackson_annotations.mustache
