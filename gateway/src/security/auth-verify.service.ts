import { Injectable, Logger } from '@nestjs/common';

interface VerifyResponse {
  valid: boolean;
  userId?: string;
  email?: string;
  roles?: string[];
  reason?: string;
}

interface CacheEntry {
  response: VerifyResponse;
  timestamp: number;
}

@Injectable()
export class AuthVerifyService {
  private readonly logger = new Logger(AuthVerifyService.name);
  private readonly cache = new Map<string, CacheEntry>();
  private readonly cacheTtlMs = 10_000; // 10 seconds
  private readonly authServiceUrl: string;

  constructor() {
    this.authServiceUrl = process.env.AUTH_SERVICE_URL || 'http://localhost:8084';
  }

  async verify(token: string): Promise<VerifyResponse | null> {
    // Check cache first
    const cached = this.cache.get(token);
    if (cached && Date.now() - cached.timestamp < this.cacheTtlMs) {
      this.logger.debug('Cache hit for token verification');
      return cached.response;
    }

    try {
      // Call Auth Service /auth/verify endpoint
      const response = await fetch(`${this.authServiceUrl}/auth/verify`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ token }),
      });

      if (!response.ok) {
        this.logger.warn(`Auth Service returned status ${response.status}`);
        return null;
      }

      const data = await response.json() as VerifyResponse;

      // Cache the result
      this.cache.set(token, {
        response: data,
        timestamp: Date.now(),
      });

      // Clean up expired cache entries periodically
      this.cleanupCache();

      return data;
    } catch (error) {
      this.logger.error('Failed to verify token with Auth Service', error);
      return null;
    }
  }

  private cleanupCache(): void {
    const now = Date.now();
    for (const [token, entry] of this.cache.entries()) {
      if (now - entry.timestamp >= this.cacheTtlMs) {
        this.cache.delete(token);
      }
    }
  }
}
