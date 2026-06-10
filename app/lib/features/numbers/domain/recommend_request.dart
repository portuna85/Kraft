import 'package:freezed_annotation/freezed_annotation.dart';

part 'recommend_request.freezed.dart';
part 'recommend_request.g.dart';

@freezed
class RecommendRequest with _$RecommendRequest {
  const factory RecommendRequest({
    @Default(5) int count,
    int? oddCount,
    int? sumMin,
    int? sumMax,
    @Default([]) List<String> disabledRules,
  }) = _RecommendRequest;

  factory RecommendRequest.fromJson(Map<String, dynamic> json) =>
      _$RecommendRequestFromJson(json);
}
