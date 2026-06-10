import 'package:freezed_annotation/freezed_annotation.dart';

part 'recommended_numbers.freezed.dart';
part 'recommended_numbers.g.dart';

@freezed
class RecommendedNumbers with _$RecommendedNumbers {
  const factory RecommendedNumbers({
    required List<List<int>> combinations,
    required List<String> appliedRules,
  }) = _RecommendedNumbers;

  factory RecommendedNumbers.fromJson(Map<String, dynamic> json) =>
      _$RecommendedNumbersFromJson(json);
}
