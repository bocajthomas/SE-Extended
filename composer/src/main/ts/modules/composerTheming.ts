import { assetCatalog, jsx, style } from "../composer";
import { defineModule } from "../types";
import { interceptComponent } from "../utils"
// Composer theme test
export default defineModule({
  name: "Theme Test",
  init() {
    const baseStyle = new style.Style({
      backgroundColor: "#ffffff",
      background: "#000000", 
    });

    interceptComponent(
      "context_chrome_header/src/ChromeHeaderRenderer",
      "ChromeHeaderRenderer",
      {
        onRenderBaseHeader: (config, _args, render) => {
          render();
          jsx.beginRender(jsx.makeNodePrototype("View"));
          jsx.setAttributeStyle("style", baseStyle);
          jsx.endRender();
        },
      }
    );
  },
  enabled: true,
});
