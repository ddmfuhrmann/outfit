# API Test Files

This directory contains HTTP client test files for IntelliJ IDEA to test the Outfit API endpoints.

## Files Overview

- `http-client.env.json` - Environment configuration with host URL
- `auth.http` - Authentication endpoints
- `users.http` - User management endpoints
- `company.http` - Company information endpoints
- `cities.http` - City management endpoints

## How to Use

1. Open IntelliJ IDEA
2. Navigate to any .http file in this directory
3. Click the "Send" button or use the shortcut (Ctrl+Shift+Alt+R on Windows/Linux, Cmd+Shift+Alt+R on Mac)
4. The requests will automatically use the configured environment variables
5. For authentication, run the `auth.http` file first to get and store the token

## Environment Setup

The files use environment variables defined in `http-client.env.json`:
- `host`: Base URL of the API
- `token`: JWT token for authentication (automatically populated after login)

## Notes

- All endpoints require authentication with a valid JWT token
- The token is automatically stored and reused across requests
- The token needs to be refreshed after each login or when it expires