(ns powerblog.gfm-alerts
  (:require
   [clojure.string :as str])
  (:import
   [com.vladsch.flexmark.ast BlockQuote HardLineBreak Paragraph SoftLineBreak]
   [com.vladsch.flexmark.html HtmlRenderer$HtmlRendererExtension HtmlWriter]
   [com.vladsch.flexmark.html.renderer
    NodeRenderer
    NodeRendererContext
    NodeRendererFactory
    NodeRenderingHandler
    NodeRenderingHandler$CustomNodeRenderer]))

(def ^:private alert-re #"(?i)\[!(NOTE|TIP|IMPORTANT|WARNING|CAUTION)\]")

(defn- alert-type
  "Returns [marker-node type-string] if the blockquote's first line is a
   GitHub alert marker (`[!NOTE]` etc.), else nil."
  [^BlockQuote node]
  (let [first-child (.getFirstChild node)]
    (when (instance? Paragraph first-child)
      (when-let [first-inline (.getFirstChild first-child)]
        (let [text (str/trim (str (.getChars first-inline)))]
          (when-let [[_ t] (re-matches alert-re text)]
            [first-inline (str/lower-case t)]))))))

(defn- render-blockquote [^BlockQuote node ^NodeRendererContext context ^HtmlWriter html]
  (if-let [[marker-node type] (alert-type node)]
    (let [after (.getNext marker-node)]
      (.unlink marker-node)
      (when (or (instance? SoftLineBreak after) (instance? HardLineBreak after))
        (.unlink after))
      (.. html (attr "class" (str "not-prose admonition " type)) (withAttr)
          (tagLineIndent "div" (fn [] (.renderChildren context node)))))
    (.delegateRender context)))

(def ^:private node-renderer
  (reify NodeRenderer
    (getNodeRenderingHandlers [_]
      (hash-set
       (NodeRenderingHandler.
        BlockQuote
        (reify NodeRenderingHandler$CustomNodeRenderer
          (render [_ node ctx html] (render-blockquote node ctx html))))))))

(def ^:private node-renderer-factory
  (proxy [NodeRendererFactory] []
    (apply [_options] node-renderer)))

(defn github-alerts-extension []
  (reify HtmlRenderer$HtmlRendererExtension
    (rendererOptions [_ _options])
    (extend [_ builder _renderer-type]
      (when (.isRendererType builder "HTML")
        (.nodeRendererFactory builder node-renderer-factory)))))
