import { Module } from '@nestjs/common';
import { GatewayModule } from './gateway/gateway.module';
import { SecurityModule } from './security/security.module';
import { HealthModule } from './health/health.module';

@Module({
  imports: [GatewayModule, SecurityModule, HealthModule],
})
export class AppModule {}
