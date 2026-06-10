class ApiResponse<T> {
  const ApiResponse({
    required this.success,
    this.data,
    this.error,
  });
  final bool success;
  final T? data;
  final ApiError? error;
}

class ApiError {
  const ApiError({
    required this.code,
    required this.message,
    this.retryable = false,
  });
  final String code;
  final String message;
  final bool retryable;

  factory ApiError.fromJson(Map<String, dynamic> json) => ApiError(
        code: json['code'] as String? ?? '',
        message: json['message'] as String? ?? '',
        retryable: json['retryable'] as bool? ?? false,
      );
}
