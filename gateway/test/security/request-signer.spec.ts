import { Test, TestingModule } from '@nestjs/testing';
import * as crypto from 'crypto';
import { RequestSigner } from '../../src/security/request-signer';

describe('RequestSigner', () => {
  let service: RequestSigner;
  const originalEnv = process.env;

  beforeEach(async () => {
    // Set a test HMAC_SECRET
    process.env = { ...originalEnv };
    process.env.HMAC_SECRET = 'test-secret-key-12345';

    const module: TestingModule = await Test.createTestingModule({
      providers: [RequestSigner],
    }).compile();

    service = module.get<RequestSigner>(RequestSigner);
  });

  afterEach(() => {
    process.env = originalEnv;
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  describe('sign', () => {
    it('should sign data with HMAC-SHA256', () => {
      const data = 'user-123:customer,admin:req-456:1234567890';
      const signature = service.sign(data);

      // Verify it's a valid hex string
      expect(signature).toMatch(/^[a-f0-9]{64}$/);

      // Verify it matches expected HMAC
      const expectedSignature = crypto
        .createHmac('sha256', 'test-secret-key-12345')
        .update(data)
        .digest('hex');

      expect(signature).toBe(expectedSignature);
    });

    it('should produce consistent signatures for same data', () => {
      const data = 'user-123:customer:req-456:1234567890';
      const signature1 = service.sign(data);
      const signature2 = service.sign(data);

      expect(signature1).toBe(signature2);
    });

    it('should produce different signatures for different data', () => {
      const data1 = 'user-123:customer:req-456:1234567890';
      const data2 = 'user-456:admin:req-789:9876543210';

      const signature1 = service.sign(data1);
      const signature2 = service.sign(data2);

      expect(signature1).not.toBe(signature2);
    });

    it('should throw error when HMAC_SECRET is not set', () => {
      delete process.env.HMAC_SECRET;

      // Create new instance without secret
      const newService = new RequestSigner();

      expect(() => newService.sign('test-data')).toThrow(
        'HMAC_SECRET environment variable must be set for request signing.',
      );
    });

    it('should handle empty strings', () => {
      const signature = service.sign('');

      // Verify it's a valid hex string
      expect(signature).toMatch(/^[a-f0-9]{64}$/);

      // Verify it matches expected HMAC
      const expectedSignature = crypto
        .createHmac('sha256', 'test-secret-key-12345')
        .update('')
        .digest('hex');

      expect(signature).toBe(expectedSignature);
    });

    it('should handle special characters in data', () => {
      const data = 'user@123:admin,customer:req-456:!@#$%^&*()';
      const signature = service.sign(data);

      expect(signature).toMatch(/^[a-f0-9]{64}$/);
    });
  });
});

