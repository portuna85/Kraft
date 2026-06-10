import 'package:freezed_annotation/freezed_annotation.dart';

part 'round.freezed.dart';
part 'round.g.dart';

@freezed
class Round with _$Round {
  const factory Round({
    required int round,
    required String drawDate,
    required List<int> numbers,
    required int bonusNumber,
    required int firstPrize,
    required int firstWinners,
    required int totalSales,
    required int secondPrize,
    required int secondWinners,
    String? fetchedAt,
  }) = _Round;

  factory Round.fromJson(Map<String, dynamic> json) => _$RoundFromJson(json);
}

extension RoundX on Round {
  List<int> get mainNumbers => numbers;
}
