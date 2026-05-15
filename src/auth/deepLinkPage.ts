import { Response } from 'express';

/**
 * Chrome (e a maioria dos navegadores modernos) bloqueiam redirects HTTP/HTTPS
 * para custom schemes (lumme://...). A solução é servir uma página HTML que
 * dispara o deep link via JavaScript — que o Chrome trata como "navegação iniciada
 * pelo usuário" e abre o app normalmente.
 *
 * A página também exibe um botão de fallback caso o auto-redirect falhe.
 */
export function renderDeepLinkRedirect(res: Response, deepLinkUrl: string, options?: {
  title?: string;
  message?: string;
}) {
  const title = options?.title ?? 'Conectando ao Lumme...';
  const message = options?.message ?? 'Voltando ao aplicativo...';
  const safeUrl = deepLinkUrl.replace(/'/g, "\\'");

  const html = `<!DOCTYPE html>
<html lang="pt-br">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta http-equiv="refresh" content="0; url=${deepLinkUrl}">
<title>${title}</title>
<style>
  * { box-sizing: border-box; }
  html, body { margin: 0; padding: 0; height: 100%; }
  body {
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    background: linear-gradient(135deg, #2E7D32 0%, #1B5E20 100%);
    color: white;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 24px;
    text-align: center;
  }
  .logo {
    font-size: 72px;
    margin-bottom: 24px;
    animation: pulse 1.5s ease-in-out infinite;
  }
  @keyframes pulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.5; }
  }
  h1 { font-size: 24px; margin: 0 0 12px; font-weight: 600; }
  p { font-size: 16px; opacity: 0.9; margin: 0 0 32px; }
  .btn {
    display: inline-block;
    background: white;
    color: #2E7D32;
    padding: 16px 32px;
    border-radius: 12px;
    text-decoration: none;
    font-weight: 600;
    font-size: 16px;
    box-shadow: 0 4px 12px rgba(0,0,0,0.2);
    transition: transform 0.1s;
  }
  .btn:active { transform: scale(0.97); }
  .small { font-size: 13px; opacity: 0.7; margin-top: 24px; }
</style>
</head>
<body>
  <div class="logo">⚡</div>
  <h1>${title}</h1>
  <p>${message}</p>
  <a href="${deepLinkUrl}" class="btn" id="openApp">Abrir no aplicativo</a>
  <p class="small">Se o app não abrir automaticamente, toque no botão acima.</p>
  <script>
    (function() {
      var url = '${safeUrl}';
      window.location.replace(url);
    })();
  </script>
</body>
</html>`;

  res.status(200).type('html').send(html);
}