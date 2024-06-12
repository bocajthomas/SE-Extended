import { assetCatalog, jsx, style } from "../composer";
import { defineModule } from "../types";
import { interceptComponent } from "../utils"
// Composer theme test
export default defineModule({
  name: "Theme Test",
  init() {
    interceptComponent(
      "context_chrome_header/src/ChromeHeaderRenderer",
      "ChromeHeaderRenderer",
      {
        onRenderBaseHeader: (config: Config, _args, render) => {
          const baseStyle = new style.Style({
            backgroundColor: "#ffffff",
            background: "#000000",
          });
          render();
          jsx.beginRender(jsx.makeNodePrototype("View"));
          jsx.setAttributeStyle("style", baseStyle);
          jsx.endRender();
          return true; 
        },
      }
    );
  },
  enabled: true,
});
