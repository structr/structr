window.onload = function() {
  //<editor-fold desc="Changeable Configuration Block">

  // the following lines will be replaced by docker/configurator, when it runs in a docker-container
  window.ui = SwaggerUIBundle({
    url: location.origin + "/structr/openapi/schema.json",
    dom_id: '#swagger-ui',
    deepLinking: true,
    presets: [
      SwaggerUIBundle.presets.apis,
      SwaggerUIStandalonePreset
    ],
    plugins: [
      SwaggerUIBundle.plugins.DownloadUrl,
      function FilterRemoteUrlParameters() {
        return {
          statePlugins: {
            spec: {
              wrapActions: {
                // Remove the ?url parameter from loading an external OpenAPI definition.
                updateUrl: (oriAction) => (payload) => {
                  const url = new URL(window.location.href);
                  if (url.searchParams.has('url')) {
                    const requestedUrl = new URL(url.searchParams.get('url'));
                    if (window.origin !== requestedUrl.origin) {
                      url.searchParams.delete('url');
                      location.replace(url.toString());
                    }
                  }
                  return oriAction(payload)
                }
              }
            }
          }
        }
      }
    ],
    layout: "BaseLayout",     // use layout: "StandaloneLayout" to add the top bar
    queryConfigEnabled: true,
    defaultModelsExpandDepth: 3
  });

  //</editor-fold>
};
