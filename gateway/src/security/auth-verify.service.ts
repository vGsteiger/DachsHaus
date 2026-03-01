import { Injectable } from '@nestjs/common';

@Injectable()
export class AuthVerifyService {
  async verify(token: string): Promise<any> {
    // Verify token with Auth Service
    return null;
  }
}
