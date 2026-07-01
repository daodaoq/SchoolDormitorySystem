import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import App from './App';
import './index.css';
import './App.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ConfigProvider
      locale={zhCN}
      theme={{
        token: {
          /* Capsule 设计令牌 → Ant Design 5 主题 */
          colorPrimary: '#E85D4E',           // --coral
          colorSuccess: '#C4D94E',           // --lime
          colorWarning: '#F2D160',           // --yellow
          colorError: '#E85D4E',             // --coral
          colorInfo: '#8BB4F7',              // --sky

          /* 字体 */
          fontFamily: "'Space Grotesk', -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif",

          /* 圆角 — 药丸级 */
          borderRadius: 16,
          borderRadiusLG: 24,
          borderRadiusSM: 12,

          /* 边框 — 外层 2px，内部表格/输入框 1px 浅色 */
          lineWidth: 1,
          colorBorder: 'rgba(26,26,26,0.15)',
          colorBorderSecondary: 'rgba(26,26,26,0.08)',

          /* 背景 */
          colorBgLayout: '#F5F5F0',
          colorBgContainer: '#FFFFFF',
          colorBgElevated: '#FFFFFF',

          /* 文字 */
          colorText: '#1A1A1A',
          colorTextSecondary: 'rgba(26,26,26,0.55)',
          colorTextTertiary: 'rgba(26,26,26,0.35)',

          /* 阴影 */
          boxShadow: '6px 6px 0 rgba(26,26,26,0.08)',
          boxShadowSecondary: '4px 4px 0 rgba(26,26,26,0.06)',

          /* 控件高度 */
          controlHeight: 40,
          controlHeightLG: 48,
          controlHeightSM: 32,

          /* 间距 — 呼吸感 */
          padding: 20,
          paddingLG: 28,
          paddingSM: 14,
          paddingXS: 10,
          marginXS: 6,
          marginSM: 12,
          marginLG: 28,
        },
        components: {
          Button: {
            borderRadius: 9999,
            borderRadiusLG: 9999,
            borderRadiusSM: 9999,
            primaryShadow: '3px 3px 0 rgba(26,26,26,0.06)',
            defaultShadow: '2px 2px 0 rgba(26,26,26,0.04)',
            dangerShadow: '3px 3px 0 rgba(26,26,26,0.06)',
            contentFontSize: 14,
            contentFontSizeLG: 16,
            contentFontSizeSM: 12,
            paddingInline: 20,
            paddingInlineLG: 28,
            paddingInlineSM: 14,
          },
          Card: {
            borderRadiusLG: 32,
            paddingLG: 32,
            padding: 24,
            boxShadow: '4px 4px 0 rgba(26,26,26,0.05)',
          },
          Table: {
            borderRadiusLG: 24,
            headerBg: '#FFFFFF',
            headerColor: '#1A1A1A',
            rowHoverBg: 'rgba(232,93,78,0.04)',
            borderColor: 'rgba(26,26,26,0.10)',
            cellPaddingBlock: 14,
            cellPaddingInline: 18,
          },
          Input: {
            borderRadius: 6,
            borderRadiusLG: 6,
            borderRadiusSM: 6,
            activeShadow: '0 0 0 2px rgba(232,93,78,0.12)',
            paddingInline: 16,
            paddingBlock: 8,
          },
          Select: {
            borderRadius: 6,
            borderRadiusLG: 6,
            borderRadiusSM: 6,
          },
          Modal: {
            borderRadiusLG: 32,
            titleFontSize: 20,
            paddingLG: 32,
          },
          Tag: {
            borderRadiusSM: 9999,
          },
          Menu: {
            itemBorderRadius: 9999,
            subMenuItemBorderRadius: 9999,
            itemMarginInline: 6,
          },
        },
      }}
    >
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </ConfigProvider>
  </React.StrictMode>
);
