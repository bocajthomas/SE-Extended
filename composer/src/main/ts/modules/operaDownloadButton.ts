import { assetCatalog, jsx, style } from "../composer"
import { defineModule } from "../types"
import { downloadLastOperaMedia } from "../imports"
import { interceptComponent } from "../utils"


export default defineModule({
    name: "Opera Download Button",
    enabled: config => config.operaDownloadButton,
    init() {
        const downloadIcon = assetCatalog.loadCatalog("share_sheet/res").downloadIcon
        interceptComponent(
            'context_chrome_header/src/ChromeHeaderRenderer',
            'ChromeHeaderRenderer',
            {
                onRenderBaseHeader: (_component: any, _args: any[], render: () => void) => {
                    render()
                    jsx.beginRender(jsx.makeNodePrototype("image"))
                    jsx.setAttributeStyle("style", new style.Style({
                        height: 32,
                        marginTop: 4,
                        marginLeft: 8,
                        marginRight: 12,
                        objectFit: "contain",
                        tint: "white"
                    }))
                    jsx.setAttribute("src", downloadIcon)
                    jsx.setAttributeFunction("onTap", () => downloadLastOperaMedia(false))
                    jsx.setAttributeFunction("onLongPress", () => downloadLastOperaMedia(true))
                    jsx.endRender()
                }
            }
        )
    }
})