import { Module } from '@nestjs/common';

import { GatewayModule } from './gateway/gateway.module';
import { HealthModule } from './health/health.module';
import { SecurityModule } from './security/security.module';

@Module({
  imports: [GatewayModule, SecurityModule, HealthModule],
})
export class AppModule {}
