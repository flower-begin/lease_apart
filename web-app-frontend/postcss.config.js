module.exports = {
  plugins: {
    tailwindcss: {},

    // =========================================================================
    //  移动端 vw 适配（默认关闭）
    //
    //  为什么关掉：
    //    vw 单位只认「视口宽度」，1vw = 窗口宽度 / 100。
    //    设计稿是按 375px 做的，一旦打开这个插件，所有 px 都会变成 vw：
    //      手机上（375~430px）：1px 设计稿 ≈ 1px        正常
    //      桌面宽屏（1680px）  ：1px 设计稿 ≈ 4.48px   整页放大 4.48 倍 ← 问题所在
    //    而且这个放大没法用 CSS 修（max-width 管不住 vw），
    //    所以要桌面正常显示，只能改成 px + 居中限宽（见 src/styles/desktop.less）。
    //
    //  想恢复真机等比缩放：
    //    把下面这段的注释去掉即可。注意恢复后桌面宽窗口会再次被放大，
    //    此时桌面预览请用 F12 设备模拟（Ctrl+Shift+M），或打开 /preview.html。
    // =========================================================================
    // "cnjm-postcss-px-to-viewport": {
    //   viewportWidth: 375, // 根据设计稿设定
    //   minPixelValue: 1,   // 最小的转换数值
    //   unitPrecision: 2    // 转化精度，转换后保留位数
    // },

    autoprefixer: {
      overrideBrowserslist: ["Android >= 4.0", "iOS >= 7"]
    }
  }
};
