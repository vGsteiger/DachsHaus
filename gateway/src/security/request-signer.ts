import * as crypto from 'crypto';

import { Injectable } from '@nestjs/common';

@Injectable()
export class RequestSigner {
  sign(data: string): string {
    const secret = process.env.HMAC_SECRET;
    if (!secret) {
      throw new Error('HMAC_SECRET environment variable must be set for request signing.');
    }
    return crypto.createHmac('sha256', secret).update(data).digest('hex');
  }
}
