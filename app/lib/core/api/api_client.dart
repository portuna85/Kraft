import 'package:dio/dio.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../config/app_config.dart';
import '../../features/rounds/domain/round.dart';
import '../../features/rounds/domain/page_result.dart';
import '../../features/stats/domain/frequency_stats.dart';
import '../../features/stats/domain/pattern_stats.dart';
import '../../features/stats/domain/companion_stats.dart';
import '../../features/stats/domain/analysis_result.dart';
import '../../features/numbers/domain/recommended_numbers.dart';
import '../../features/numbers/domain/recommend_request.dart';
import 'api_response.dart';

part 'api_client.g.dart';

@riverpod
KraftApiClient kraftApiClient(KraftApiClientRef ref) {
  final config = ref.watch(appConfigProvider);
  final dio = Dio(BaseOptions(
    baseUrl: config.baseUrl,
    connectTimeout: const Duration(seconds: 10),
    receiveTimeout: const Duration(seconds: 15),
    headers: {
      if (config.apiKey != null) 'X-Api-Key': config.apiKey!,
      'Accept': 'application/json',
    },
  ));
  return KraftApiClient(dio);
}

class KraftApiClient {
  KraftApiClient(this._dio);
  final Dio _dio;

  // 회차
  Future<ApiResponse<Round>> getLatestRound() =>
      _get('/api/v1/rounds/latest', (j) => Round.fromJson(j));

  Future<ApiResponse<Round>> getRound(int round) =>
      _get('/api/v1/rounds/$round', (j) => Round.fromJson(j));

  Future<ApiResponse<PageResult<Round>>> listRounds(int page, int size) =>
      _get(
        '/api/v1/rounds',
        (j) => PageResult.fromJson(j, (e) => Round.fromJson(e as Map<String, dynamic>)),
        queryParameters: {'page': page, 'size': size},
      );

  // 통계
  Future<ApiResponse<List<BallFrequency>>> getFrequency({int? period}) =>
      _getList(
        '/api/v1/stats/frequency',
        (j) => BallFrequency.fromJson(j),
        queryParameters: period != null ? {'period': period} : null,
      );

  Future<ApiResponse<PatternStats>> getPatterns() =>
      _get('/api/v1/stats/patterns', (j) => PatternStats.fromJson(j));

  Future<ApiResponse<List<CompanionEntry>>> getCompanion(int target) =>
      _getList(
        '/api/v1/stats/companion',
        (j) => CompanionEntry.fromJson(j),
        queryParameters: {'target': target},
      );

  Future<ApiResponse<AnalysisResult>> analyze(Map<String, dynamic> body) =>
      _post('/api/v1/stats/analysis', body, (j) => AnalysisResult.fromJson(j));

  // 번호 추천
  Future<ApiResponse<RecommendedNumbers>> recommend(RecommendRequest request) =>
      _post('/api/v1/numbers/recommend', request.toJson(),
          (j) => RecommendedNumbers.fromJson(j));

  // ── 내부 헬퍼 ──────────────────────────────────────────────────────────

  Future<ApiResponse<T>> _get<T>(
    String path,
    T Function(Map<String, dynamic>) fromJson, {
    Map<String, dynamic>? queryParameters,
  }) async {
    final res = await _dio.get<Map<String, dynamic>>(
      path,
      queryParameters: queryParameters,
    );
    return _decode(res.data!, fromJson);
  }

  Future<ApiResponse<List<T>>> _getList<T>(
    String path,
    T Function(Map<String, dynamic>) fromJson, {
    Map<String, dynamic>? queryParameters,
  }) async {
    final res = await _dio.get<Map<String, dynamic>>(
      path,
      queryParameters: queryParameters,
    );
    final envelope = res.data!;
    final List<T> items = (envelope['data'] as List)
        .map((e) => fromJson(e as Map<String, dynamic>))
        .toList();
    return ApiResponse<List<T>>(
      success: envelope['success'] as bool? ?? true,
      data: items,
      error: _parseError(envelope),
    );
  }

  Future<ApiResponse<T>> _post<T>(
    String path,
    Map<String, dynamic> body,
    T Function(Map<String, dynamic>) fromJson,
  ) async {
    final res = await _dio.post<Map<String, dynamic>>(path, data: body);
    return _decode(res.data!, fromJson);
  }

  ApiResponse<T> _decode<T>(
    Map<String, dynamic> envelope,
    T Function(Map<String, dynamic>) fromJson,
  ) {
    return ApiResponse<T>(
      success: envelope['success'] as bool? ?? true,
      data: envelope['data'] != null
          ? fromJson(envelope['data'] as Map<String, dynamic>)
          : null,
      error: _parseError(envelope),
    );
  }

  ApiError? _parseError(Map<String, dynamic> envelope) {
    final e = envelope['error'];
    if (e == null) return null;
    return ApiError.fromJson(e as Map<String, dynamic>);
  }
}
