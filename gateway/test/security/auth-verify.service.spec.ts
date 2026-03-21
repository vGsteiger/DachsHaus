import { Test, TestingModule } from '@nestjs/testing';
import { AuthVerifyService } from '../../src/security/auth-verify.service';

describe('AuthVerifyService', () => {
  let service: AuthVerifyService;
  let originalFetch: typeof global.fetch;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [AuthVerifyService],
    }).compile();

    service = module.get<AuthVerifyService>(AuthVerifyService);
    originalFetch = global.fetch;
  });

  afterEach(() => {
    global.fetch = originalFetch;
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  describe('verify', () => {
    it('should return valid response when token is valid', async () => {
      const mockResponse = {
        valid: true,
        userId: 'user-123',
        email: 'test@example.com',
        roles: ['customer'],
      };

      global.fetch = jest.fn().mockResolvedValue({
        ok: true,
        json: async () => mockResponse,
      } as Response);

      const result = await service.verify('valid-token');

      expect(result).toEqual(mockResponse);
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('/auth/verify'),
        expect.objectContaining({
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ token: 'valid-token' }),
        }),
      );
    });

    it('should return invalid response when token is invalid', async () => {
      const mockResponse = {
        valid: false,
        reason: 'expired',
      };

      global.fetch = jest.fn().mockResolvedValue({
        ok: true,
        json: async () => mockResponse,
      } as Response);

      const result = await service.verify('invalid-token');

      expect(result).toEqual(mockResponse);
    });

    it('should return null when auth service returns non-200 status', async () => {
      global.fetch = jest.fn().mockResolvedValue({
        ok: false,
        status: 500,
      } as Response);

      const result = await service.verify('token');

      expect(result).toBeNull();
    });

    it('should return null when fetch throws error', async () => {
      global.fetch = jest.fn().mockRejectedValue(new Error('Network error'));

      const result = await service.verify('token');

      expect(result).toBeNull();
    });

    it('should cache verification results for 10 seconds', async () => {
      const mockResponse = {
        valid: true,
        userId: 'user-123',
        email: 'test@example.com',
        roles: ['customer'],
      };

      global.fetch = jest.fn().mockResolvedValue({
        ok: true,
        json: async () => mockResponse,
      } as Response);

      // First call
      const result1 = await service.verify('cached-token');
      expect(result1).toEqual(mockResponse);
      expect(global.fetch).toHaveBeenCalledTimes(1);

      // Second call within cache TTL - should use cache
      const result2 = await service.verify('cached-token');
      expect(result2).toEqual(mockResponse);
      expect(global.fetch).toHaveBeenCalledTimes(1); // Still only 1 call

      // Verify both results are the same
      expect(result1).toEqual(result2);
    });

    it('should not use cache for different tokens', async () => {
      const mockResponse1 = {
        valid: true,
        userId: 'user-123',
        email: 'user1@example.com',
        roles: ['customer'],
      };

      const mockResponse2 = {
        valid: true,
        userId: 'user-456',
        email: 'user2@example.com',
        roles: ['admin'],
      };

      global.fetch = jest
        .fn()
        .mockResolvedValueOnce({
          ok: true,
          json: async () => mockResponse1,
        } as Response)
        .mockResolvedValueOnce({
          ok: true,
          json: async () => mockResponse2,
        } as Response);

      const result1 = await service.verify('token-1');
      const result2 = await service.verify('token-2');

      expect(result1).toEqual(mockResponse1);
      expect(result2).toEqual(mockResponse2);
      expect(global.fetch).toHaveBeenCalledTimes(2);
    });
  });
});

