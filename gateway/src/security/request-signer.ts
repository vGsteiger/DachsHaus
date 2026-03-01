import { Injectable } from '@nestjs/common';
import * as crypto from 'crypto';

@Injectable()
export class RequestSigner {
  sign(data: string): string {
    const secret = process.env.HMAC_SECRET || 'default-secret';
    return crypto.createHmac('sha256', secret).update(data).digest('hex');
  }
}
