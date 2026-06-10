import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:retrofit/retrofit.dart';
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

@RestApi()
abstract class KraftApiClient {
  factory KraftApiClient(Dio dio, {String? baseUrl}) = _KraftApiClient;

  // 회차
  @GET('/api/v1/rounds/latest')
  Future<ApiResponse<Round>> getLatestRound();

  @GET('/api/v1/rounds/{drwNo}')
  Future<ApiResponse<Round>> getRound(@Path('drwNo') int drwNo);

  @GET('/api/v1/rounds')
  Future<ApiResponse<PageResult<Round>>> listRounds(
    @Query('page') int page,
    @Query('size') int size,
  );

  // 통계
  @GET('/api/v1/stats/frequency')
  Future<ApiResponse<FrequencyStats>> getFrequency(
    {@Query('period') int? period},
  );

  @GET('/api/v1/stats/patterns')
  Future<ApiResponse<PatternStats>> getPatterns();

  @GET('/api/v1/stats/companion')
  Future<ApiResponse<CompanionStats>> getCompanion(
    @Query('target') int target,
  );

  @POST('/api/v1/stats/analysis')
  Future<ApiResponse<AnalysisResult>> analyze(
    @Body() Map<String, dynamic> body,
  );

  // 번호 추천
  @POST('/api/v1/numbers/recommend')
  Future<ApiResponse<RecommendedNumbers>> recommend(
    @Body() RecommendRequest request,
  );
}
