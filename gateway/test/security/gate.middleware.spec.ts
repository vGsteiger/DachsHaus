import { Test, TestingModule } from '@nestjs/testing';
import { Request, Response, NextFunction } from 'express';
import { GateMiddleware } from '../../src/security/gate.middleware';
import { AuthVerifyService } from '../../src/security/auth-verify.service';
import { RequestSigner } from '../../src/security/request-signer';

describe('GateMiddleware', () => {
  let middleware: GateMiddleware;
  let authVerifyService: jest.Mocked<AuthVerifyService>;
  let requestSigner: jest.Mocked<RequestSigner>;
  let mockRequest: Partial<Request>;
  let mockResponse: Partial<Response>;
  let mockNext: NextFunction;
  let originalEnv: NodeJS.ProcessEnv;

  beforeEach(async () => {
    originalEnv = process.env;
    process.env.HMAC_SECRET = 'test-secret';

    // Create mocks
    authVerifyService = {
      verify: jest.fn(),
    } as any;

    requestSigner = {
      sign: jest.fn().mockReturnValue('mock-signature'),
    } as any;

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        GateMiddleware,
        { provide: AuthVerifyService, useValue: authVerifyService },
        { provide: RequestSigner, useValue: requestSigner },
      ],
    }).compile();

    middleware = module.get<GateMiddleware>(GateMiddleware);

    // Setup mock request/response
    mockRequest = {
      headers: {},
      body: {},
    };

    mockResponse = {
      status: jest.fn().mockReturnThis(),
      json: jest.fn().mockReturnThis(),
    };

    mockNext = jest.fn();
  });

  afterEach(() => {
    process.env = originalEnv;
  });

  it('should be defined', () => {
    expect(middleware).toBeDefined();
  });

  describe('authenticated requests', () => {
    it('should verify token and forward authenticated request', async () => {
      mockRequest.headers = { authorization: 'Bearer valid-token' };
      mockRequest.body = { query: 'query GetProfile { profile { id } }' };

      authVerifyService.verify.mockResolvedValue({
        valid: true,
        userId: 'user-123',
        email: 'test@example.com',
        roles: ['customer'],
      });

      await middleware.use(mockRequest as Request, mockResponse as Response, mockNext);

      expect(authVerifyService.verify).toHaveBeenCalledWith('valid-token');
      expect(requestSigner.sign).toHaveBeenCalled();
      expect(mockRequest.headers['x-user-id']).toBe('user-123');
      expect(mockRequest.headers['x-user-roles']).toBe('customer');
      expect(mockRequest.headers['x-request-id']).toBeDefined();
      expect(mockRequest.headers['x-gateway-signature']).toBeDefined();
      expect(mockNext).toHaveBeenCalled();
    });

    it('should handle multiple roles', async () => {
      mockRequest.headers = { authorization: 'Bearer admin-token' };
      mockRequest.body = { query: 'query GetUsers { users { id } }' };

      authVerifyService.verify.mockResolvedValue({
        valid: true,
        userId: 'admin-123',
        email: 'admin@example.com',
        roles: ['customer', 'admin'],
      });

      await middleware.use(mockRequest as Request, mockResponse as Response, mockNext);

      expect(mockRequest.headers['x-user-roles']).toBe('customer,admin');
      expect(mockNext).toHaveBeenCalled();
    });
  });

  describe('unauthenticated requests', () => {
    it('should allow public operations without token', async () => {
      mockRequest.body = { query: 'query GetProducts { products { id } }' };

      await middleware.use(mockRequest as Request, mockResponse as Response, mockNext);

      expect(authVerifyService.verify).not.toHaveBeenCalled();
      expect(mockRequest.headers['x-user-id']).toBe('');
      expect(mockRequest.headers['x-user-roles']).toBe('');
      expect(mockNext).toHaveBeenCalled();
    });

    it('should allow Login operation without token', async () => {
      mockRequest.body = { query: 'mutation Login($email: String!, $password: String!) { login(email: $email, password: $password) { token } }' };

      await middleware.use(mockRequest as Request, mockResponse as Response, mockNext);

      expect(authVerifyService.verify).not.toHaveBeenCalled();
      expect(mockNext).toHaveBeenCalled();
    });

    it('should allow Register operation without token', async () => {
      mockRequest.body = { query: 'mutation Register($input: RegisterInput!) { register(input: $input) { token } }' };

      await middleware.use(mockRequest as Request, mockResponse as Response, mockNext);

      expect(authVerifyService.verify).not.toHaveBeenCalled();
      expect(mockNext).toHaveBeenCalled();
    });

    it('should allow IntrospectionQuery without token', async () => {
      mockRequest.body = { query: 'query IntrospectionQuery { __schema { types { name } } }' };

      await middleware.use(mockRequest as Request, mockResponse as Response, mockNext);

      expect(authVerifyService.verify).not.toHaveBeenCalled();
      expect(mockNext).toHaveBeenCalled();
    });

    it('should block protected operations without token', async () => {
      mockRequest.body = { query: 'query GetProfile { profile { id } }' };

      await middleware.use(mockRequest as Request, mockResponse as Response, mockNext);

      expect(mockResponse.status).toHaveBeenCalledWith(401);
      expect(mockResponse.json).toHaveBeenCalledWith({
        errors: [
          {
            message: 'Authentication required',
            extensions: { code: 'UNAUTHENTICATED' },
          },
        ],
      });
      expect(mockNext).not.toHaveBeenCalled();
    });
  });

  describe('invalid tokens', () => {
    it('should treat invalid token as anonymous', async () => {
      mockRequest.headers = { authorization: 'Bearer invalid-token' };
      mockRequest.body = { query: 'query GetProducts { products { id } }' };

      authVerifyService.verify.mockResolvedValue({
        valid: false,
        reason: 'expired',
      });

      await middleware.use(mockRequest as Request, mockResponse as Response, mockNext);

      expect(authVerifyService.verify).toHaveBeenCalledWith('invalid-token');
      expect(mockRequest.headers['x-user-id']).toBe('');
      expect(mockRequest.headers['x-user-roles']).toBe('');
      expect(mockNext).toHaveBeenCalled();
    });

    it('should block protected operations with invalid token', async () => {
      mockRequest.headers = { authorization: 'Bearer expired-token' };
      mockRequest.body = { query: 'query GetProfile { profile { id } }' };

      authVerifyService.verify.mockResolvedValue({
        valid: false,
        reason: 'expired',
      });

      await middleware.use(mockRequest as Request, mockResponse as Response, mockNext);

      expect(mockResponse.status).toHaveBeenCalledWith(401);
      expect(mockNext).not.toHaveBeenCalled();
    });
  });

  describe('request signing', () => {
    it('should sign request with correct payload format', async () => {
      mockRequest.headers = { authorization: 'Bearer valid-token' };
      mockRequest.body = { query: 'query GetProfile { profile { id } }' };

      authVerifyService.verify.mockResolvedValue({
        valid: true,
        userId: 'user-123',
        roles: ['customer', 'admin'],
      });

      await middleware.use(mockRequest as Request, mockResponse as Response, mockNext);

      const signCall = requestSigner.sign.mock.calls[0][0];
      // Format: userId:roles:requestId:timestamp
      expect(signCall).toMatch(/^user-123:customer,admin:[a-f0-9-]+:\d+$/);
    });

    it('should include timestamp in signature header', async () => {
      mockRequest.body = { query: 'query GetProducts { products { id } }' };

      await middleware.use(mockRequest as Request, mockResponse as Response, mockNext);

      const signature = mockRequest.headers['x-gateway-signature'] as string;
      expect(signature).toMatch(/^\d+\.mock-signature$/);
    });
  });

  describe('error handling', () => {
    it('should handle errors gracefully', async () => {
      mockRequest.body = { query: 'query GetProducts { products { id } }' };
      requestSigner.sign.mockImplementation(() => {
        throw new Error('Signing error');
      });

      await middleware.use(mockRequest as Request, mockResponse as Response, mockNext);

      expect(mockResponse.status).toHaveBeenCalledWith(500);
      expect(mockResponse.json).toHaveBeenCalledWith({
        errors: [
          {
            message: 'Internal server error',
            extensions: { code: 'INTERNAL_SERVER_ERROR' },
          },
        ],
      });
      expect(mockNext).not.toHaveBeenCalled();
    });
  });
});

