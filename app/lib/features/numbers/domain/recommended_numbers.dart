import 'package:freezed_annotation/freezed_annotation.dart';

part 'recommended_numbers.freezed.dart';
part 'recommended_numbers.g.dart';

@freezed
class CombinationItem with _$CombinationItem {
  const factory CombinationItem({
    required List<int> numbers,
    @Default([]) List<String> passedLabels,
  }) = _CombinationItem;

  factory CombinationItem.fromJson(Map<String, dynamic> json) =>
      _$CombinationItemFromJson(json);
}

@freezed
class RecommendedNumbers with _$RecommendedNumbers {
  const factory RecommendedNumbers({
    required List<CombinationItem> combinations,
  }) = _RecommendedNumbers;

  factory RecommendedNumbers.fromJson(Map<String, dynamic> json) =>
      _$RecommendedNumbersFromJson(json);
}
