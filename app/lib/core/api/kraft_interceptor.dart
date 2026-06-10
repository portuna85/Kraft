import 'package:dio/dio.dart';
import '../error/app_exception.dart';

class KraftInterceptor extends Interceptor {
  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    handler.reject(err.copyWith(error: _toAppException(err)));
  }

  AppException _toAppException(DioException err) {
    switch (err.type) {
      case DioExceptionType.connectionTimeout:
      case DioExceptionType.sendTimeout:
      case DioExceptionType.receiveTimeout:
        return const NetworkException('요청 시간이 초과됐습니다. 잠시 후 다시 시도해 주세요.');
      case DioExceptionType.connectionError:
        return const NetworkException('네트워크에 연결할 수 없습니다. 인터넷 연결을 확인하세요.');
      case DioExceptionType.cancel:
        return const NetworkException('요청이 취소됐습니다.');
      case DioExceptionType.badResponse:
        return _fromHttpResponse(err.response!);
      default:
        return NetworkException(err.message ?? '알 수 없는 오류가 발생했습니다.');
    }
  }

  AppException _fromHttpResponse(Response<dynamic> response) {
    final statusCode = response.statusCode ?? 0;
    final (code, message, retryable) = _extractError(response.data);

    return switch (statusCode) {
      401 => ApiException(
          code: code ?? 'UNAUTHORIZED',
          message: message ?? '인증이 필요합니다.',
          retryable: false,
        ),
      404 => NotFoundException(message ?? '요청한 데이터를 찾을 수 없습니다.'),
      429 => ApiException(
          code: code ?? 'RATE_LIMITED',
          message: message ?? '요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.',
          retryable: true,
        ),
      >= 500 => NetworkException(
          message ?? '서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.',
        ),
      _ => ApiException(
          code: code ?? 'API_ERROR',
          message: message ?? '오류가 발생했습니다 (HTTP $statusCode).',
          retryable: retryable,
        ),
    };
  }

  (String? code, String? message, bool retryable) _extractError(dynamic data) {
    if (data is! Map<String, dynamic>) return (null, null, false);
    final error = data['error'];
    if (error is! Map<String, dynamic>) return (null, null, false);
    return (
      error['code'] as String?,
      error['message'] as String?,
      error['retryable'] as bool? ?? false,
    );
  }
}
