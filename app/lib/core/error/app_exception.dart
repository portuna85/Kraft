sealed class AppException implements Exception {
  const AppException(this.message);
  final String message;

  @override
  String toString() => message;
}

/// DioException.error 에 래핑된 AppException 또는 일반 에러에서
/// 사람이 읽을 수 있는 메시지를 추출한다.
String errorMessage(Object error) {
  if (error is AppException) return error.message;
  // DioException wraps AppException in its .error field
  final dynamic unwrapped = _unwrap(error);
  if (unwrapped is AppException) return unwrapped.message;
  final msg = error.toString();
  if (msg.startsWith('Exception: ')) return msg.substring(11);
  return msg;
}

dynamic _unwrap(Object error) {
  try {
    // DioException exposes .error; use dynamic access to avoid importing dio
    return (error as dynamic).error;
  } catch (_) {
    return null;
  }
}

final class NetworkException extends AppException {
  const NetworkException(super.message);
}

final class ApiException extends AppException {
  const ApiException({
    required this.code,
    required String message,
    required this.retryable,
  }) : super(message);
  final String code;
  final bool retryable;
}

final class NotFoundException extends AppException {
  const NotFoundException(super.message);
}
