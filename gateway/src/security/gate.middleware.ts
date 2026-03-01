import { Injectable, NestMiddleware } from '@nestjs/common';
import { Request, Response, NextFunction } from 'express';

@Injectable()
export class GateMiddleware implements NestMiddleware {
  use(req: Request, res: Response, next: NextFunction) {
    // Gate middleware logic
    next();
  }
}
