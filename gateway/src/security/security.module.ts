import { Module, NestModule, MiddlewareConsumer } from '@nestjs/common';

import { AuthVerifyService } from './auth-verify.service';
import { RequestSigner } from './request-signer';
import { GateMiddleware } from './gate.middleware';

@Module({
  providers: [AuthVerifyService, RequestSigner, GateMiddleware],
  exports: [AuthVerifyService, RequestSigner],
})
export class SecurityModule implements NestModule {
  configure(consumer: MiddlewareConsumer) {
    consumer.apply(GateMiddleware).forRoutes('*');
  }
}
