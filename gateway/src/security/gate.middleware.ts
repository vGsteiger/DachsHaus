import { Injectable, NestMiddleware, Logger } from '@nestjs/common';
import { Request, Response, NextFunction } from 'express';
import { v4 as uuidv4 } from 'uuid';

import { AuthVerifyService } from './auth-verify.service';
import { RequestSigner } from './request-signer';
import { parseOperation } from './operation-parser';
import { publicOperations } from '../gateway/public-operations';

@Injectable()
export class GateMiddleware implements NestMiddleware {
  private readonly logger = new Logger(GateMiddleware.name);

  constructor(
    private readonly authVerifyService: AuthVerifyService,
    private readonly requestSigner: RequestSigner,
  ) {}

  async use(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      // 1. Extract Authorization header
      const authHeader = req.headers.authorization;
      const token = authHeader?.startsWith('Bearer ') ? authHeader.substring(7) : null;

      // 2. Verify token if present
      let userId: string | null = null;
      let roles: string[] = [];

      if (token) {
        const verifyResponse = await this.authVerifyService.verify(token);
        if (verifyResponse?.valid) {
          userId = verifyResponse.userId || null;
          roles = verifyResponse.roles || [];
          this.logger.debug(`Authenticated user: ${userId} with roles: ${roles.join(', ')}`);
        } else {
          this.logger.debug(`Invalid token: ${verifyResponse?.reason || 'unknown'}`);
        }
      }

      // 3. Extract operation name from GraphQL query
      const body = req.body;
      const operation = body?.query ? parseOperation(body.query) : null;

      // 4. Check if operation is in public allowlist
      const isPublicOperation = operation && publicOperations.has(operation);

      // 5. Gate protected operations
      if (!isPublicOperation && !userId) {
        this.logger.warn(`Unauthorized access to protected operation: ${operation || 'unknown'}`);
        res.status(401).json({
          errors: [
            {
              message: 'Authentication required',
              extensions: { code: 'UNAUTHENTICATED' },
            },
          ],
        });
        return;
      }

      // 6. Generate request ID
      const requestId = uuidv4();

      // 7. Sign request with HMAC
      const timestamp = Date.now().toString();
      const payload = `${userId || ''}:${roles.join(',')}:${requestId}:${timestamp}`;
      const signature = this.requestSigner.sign(payload);

      // 8. Forward with security headers
      req.headers['x-gateway-signature'] = `${timestamp}.${signature}`;
      req.headers['x-user-id'] = userId || '';
      req.headers['x-user-roles'] = roles.join(',');
      req.headers['x-request-id'] = requestId;

      this.logger.debug(`Request signed and forwarded: operation=${operation}, userId=${userId}, requestId=${requestId}`);
      next();
    } catch (error) {
      this.logger.error('Error in gate middleware', error);
      res.status(500).json({
        errors: [
          {
            message: 'Internal server error',
            extensions: { code: 'INTERNAL_SERVER_ERROR' },
          },
        ],
      });
    }
  }
}
