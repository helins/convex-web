(ns convex-web.site.markdown
  (:require 
   [clojure.string :as str]
   
   [convex-web.site.gui :as gui]
   [convex-web.site.backend :as backend]
   [reagent.core :as reagent]))

(defn MarkdownPage [_ {:keys [scroll-to markdown]} _]
  (reagent/with-let [*nodes (reagent/atom nil)
                     
                     pending-scroll?-ref (reagent/atom (boolean scroll-to))]
    
    (let [{:keys [ajax/status contents toc? smart-toc?] :or {toc? true}} markdown]
      [:div.flex.flex-1
       (case status
         :ajax.status/pending
         [:div.flex.flex-1.items-center.justify-center
          [gui/Spinner]]
         
         :ajax.status/error
         [:span "Error"]
         
         :ajax.status/success
         [:<>
          ;; -- Markdown
          [:div
           {:ref
            (fn [el]
              (when el
                (let [nodes (.querySelectorAll el "h2")]
                  (when smart-toc?
                    (reset! *nodes nodes))
                  
                  (when @pending-scroll?-ref
                    (let [scroll-to (js/decodeURIComponent (str/replace scroll-to "+" " "))]
                      (when-let [el (reduce
                                      (fn [_ node]
                                        (when (= scroll-to (.-textContent node))
                                          (reduced node)))
                                      nodes)]
                        
                        (gui/scroll-element-into-view el {:behavior "smooth"})
                        
                        (reset! pending-scroll?-ref false)))))))}
           
           (for [{:keys [name content]} contents]
             ^{:key name}
             [:article.prose.prose-sm.md:prose-lg.mb-10
              {:id name}
              [gui/Markdown content]])]
          
          ;; -- On this page
          (when toc?
            (let [item-style "text-gray-600 hover:text-gray-900 cursor-pointer"]
              [:div.hidden.md:flex.md:flex-col.md:mr-10.ml-16
               [:span.text-xs.text-gray-500.font-bold.uppercase "On this Page"]
               
               [:ul.list-none.text-sm.mt-4.space-y-2.overflow-auto
                (if smart-toc?
                  ;; Smart ToC
                  (for [node @*nodes]
                    ^{:key (.-textContent node)}
                    [:li.mb-2
                     [:a.text-gray-600.hover:text-gray-900.cursor-pointer
                      {:on-click #(gui/scroll-element-into-view node)}
                      (.-textContent node)]])
                  
                  ;; Default ToC
                  (for [{:keys [name]} contents]
                    ^{:key name}
                    [:li
                     {:class item-style
                      :on-click #(gui/scroll-into-view name)}
                     name]))]]))]
         
         [:div])])))

(defn markdown-on-push
  [_ {:keys [id]} set-state]
  (set-state update :markdown assoc :ajax/status :ajax.status/pending)
  
  (backend/GET-markdown-page
    id
    {:handler
     (fn [markdown-page]
       (set-state update :markdown merge {:ajax/status :ajax.status/success} markdown-page))
     
     :error-handler
     (fn [error]
       (set-state update :markdown assoc :ajax/status :ajax.status/error :ajax/error error))}))

(def markdown-page
  #:page {:id :page.id/markdown
          :component #'MarkdownPage
          :template :developer
          :on-push markdown-on-push})

(def markdown-marketing-page
  #:page {:id :page.id/markdown-marketing
          :component #'MarkdownPage
          :template :marketing
          :on-push markdown-on-push})
