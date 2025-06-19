export default defineNitroPlugin((nitroApp) => {
    nitroApp.hooks.hook('render:html', (html, { event }) => {
      // 这将是 HTML 模板的对象表示。
      console.log(html)
      html.head.push(`<meta name="description" content="我的自定义描述" />`)
    })
    // 你也可以在这里拦截响应。
    nitroApp.hooks.hook('render:response', (response, { event }) => { console.log(response) })
  })
  