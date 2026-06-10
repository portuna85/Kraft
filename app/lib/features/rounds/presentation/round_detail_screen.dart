import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../data/rounds_repository.dart';
import '../domain/round.dart';

part 'round_detail_screen.g.dart';

@riverpod
Future<Round> roundDetail(RoundDetailRef ref, int drwNo) =>
    ref.watch(roundsRepositoryProvider).getByDrwNo(drwNo);

final _fmt = NumberFormat('#,###');

class RoundDetailScreen extends ConsumerWidget {
  const RoundDetailScreen({super.key, required this.drwNo});
  final int drwNo;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(roundDetailProvider(drwNo));
    return Scaffold(
      appBar: AppBar(title: Text('제 $drwNo회')),
      body: async.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text(e.toString())),
        data: (round) => _RoundDetail(round: round),
      ),
    );
  }
}

class _RoundDetail extends StatelessWidget {
  const _RoundDetail({required this.round});
  final Round round;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text(
          '제 ${round.round}회 (${round.drawDate})',
          style: theme.textTheme.titleLarge,
        ),
        const SizedBox(height: 20),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              children: [
                const Text('당첨번호'),
                const SizedBox(height: 12),
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    ...round.mainNumbers.map((n) => Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 4),
                          child: _Ball(n, isMain: true),
                        )),
                    const Padding(
                      padding: EdgeInsets.symmetric(horizontal: 8),
                      child: Text('+', style: TextStyle(fontSize: 20)),
                    ),
                    _Ball(round.bonusNumber, isMain: false),
                  ],
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 16),
        Card(
          child: Column(
            children: [
              _InfoRow('1등 당첨자 수', '${round.firstWinners}명'),
              _InfoRow('1등 당첨금', '${_fmt.format(round.firstPrize)}원'),
              _InfoRow('2등 당첨자 수', '${round.secondWinners}명'),
              _InfoRow('총 판매금액', '${_fmt.format(round.totalSales)}원'),
            ],
          ),
        ),
      ],
    );
  }
}

class _InfoRow extends StatelessWidget {
  const _InfoRow(this.label, this.value);
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      title: Text(label),
      trailing: Text(value, style: const TextStyle(fontWeight: FontWeight.w600)),
    );
  }
}

class _Ball extends StatelessWidget {
  const _Ball(this.number, {required this.isMain});
  final int number;
  final bool isMain;

  Color get _color {
    if (number <= 10) return const Color(0xFFF9A825);
    if (number <= 20) return const Color(0xFF42A5F5);
    if (number <= 30) return const Color(0xFFEF5350);
    if (number <= 40) return const Color(0xFF757575);
    return const Color(0xFF66BB6A);
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      width: isMain ? 44 : 40,
      height: isMain ? 44 : 40,
      decoration: BoxDecoration(
        color: isMain ? _color : Colors.grey.shade300,
        shape: BoxShape.circle,
      ),
      alignment: Alignment.center,
      child: Text(
        '$number',
        style: TextStyle(
          color: isMain ? Colors.white : Colors.black87,
          fontSize: isMain ? 16 : 14,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }
}
