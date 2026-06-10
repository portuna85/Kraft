sealed class AppException implements Exception {
  const AppException(this.message);
  final String message;
}

final class NetworkException extends AppException {
  const NetworkException(super.message);
}

final class ApiException extends AppException {
  const ApiException({required this.code, required super.message, required this.retryable});
  final String code;
  final bool retryable;
}

final class NotFoundException extends AppException {
  const NotFoundException(super.message);
}
