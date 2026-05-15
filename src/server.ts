import { createApp } from './app';
import { env } from './config/env';
import { prisma } from './prisma';

async function main() {
  // Testa conexão com o banco
  await prisma.$connect();
  console.log('[db] conectado');

  const app = createApp();
  const server = app.listen(env.PORT, () => {
    const mode = env.MOCK_SMARTTHINGS ? 'MOCK' : 'REAL';
    console.log(`\n┌──────────────────────────────────────────────────┐`);
    console.log(`│  🌿  Lumme Backend                                │`);
    console.log(`│  Porta: ${String(env.PORT).padEnd(40)} │`);
    console.log(`│  Ambiente: ${env.NODE_ENV.padEnd(37)} │`);
    console.log(`│  SmartThings: ${mode.padEnd(34)} │`);
    console.log(`│  Base URL: http://localhost:${String(env.PORT).padEnd(20)} │`);
    console.log(`└──────────────────────────────────────────────────┘\n`);
  });

  // Shutdown gracioso
  const shutdown = async (signal: string) => {
    console.log(`\n[${signal}] encerrando...`);
    server.close();
    await prisma.$disconnect();
    process.exit(0);
  };
  process.on('SIGINT', () => shutdown('SIGINT'));
  process.on('SIGTERM', () => shutdown('SIGTERM'));
}

main().catch((e) => {
  console.error('[fatal]', e);
  process.exit(1);
});
