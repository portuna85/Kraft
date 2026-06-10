import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../rounds/data/rounds_repository.dart';
import '../../rounds/domain/round.dart';
import '../../rounds/presentation/rounds_screen.dart';
import '../../numbers/presentation/numbers_screen.dart';

part 'home_screen.g.dart';

@riverpod
Future<Round> latestRound(LatestRoundRef ref) =>
    ref.watch(roundsRepositoryProvider).getLatest();

class HomeScreen extends ConsumerWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Scaffold(
      appBar: AppBar(title: const Text('KRAFT Lotto')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _LatestRoundCard(),
          const SizedBox(height: 16),
          const _RecommendCard(),
        ],
      ),
    );
  }
}

class _LatestRoundCard extends ConsumerWidget {
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(latestRoundProvider);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: async.when(
          loading: () => const SizedBox(
            height: 80,
            child: Center(child: CircularProgressIndicator()),
          ),
          error: (_, __) => const Text('최신 회차를 불러올 수 없습니다'),
          data: (round) => Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Text(
                    '제 ${round.drwNo}회',
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                  const Spacer(),
                  TextButton(
                    onPressed: () => context.go('/rounds/${round.drwNo}'),
                    child: const Text('상세보기'),
                  ),
                ],
              ),
              Text(round.drwNoDate,
                  style: Theme.of(context).textTheme.bodySmall),
              const SizedBox(height: 12),
              Wrap(
                spacing: 6,
                children: [
                  ...round.mainNumbers.map((n) => _Ball(n)),
                  const _PlusSeparator(),
                  _Ball(round.bnusNo, isBonus: true),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _RecommendCard extends StatelessWidget {
  const _RecommendCard();

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('번호 추천', style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 8),
            const Text('AI 기반 통계 분석으로 번호 조합을 생성합니다.'),
            const SizedBox(height: 12),
            SizedBox(
              width: double.infinity,
              child: FilledButton.icon(
                onPressed: () => context.go('/numbers'),
                icon: const Icon(Icons.auto_awesome),
                label: const Text('번호 추천받기'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _Ball extends StatelessWidget {
  const _Ball(this.number, {this.isBonus = false});
  final int number;
  final bool isBonus;

  Color get _color {
    if (isBonus) return Colors.grey.shade400;
    if (number <= 10) return const Color(0xFFF9A825);
    if (number <= 20) return const Color(0xFF42A5F5);
    if (number <= 30) return const Color(0xFFEF5350);
    if (number <= 40) return const Color(0xFF757575);
    return const Color(0xFF66BB6A);
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 36,
      height: 36,
      decoration: BoxDecoration(color: _color, shape: BoxShape.circle),
      alignment: Alignment.center,
      child: Text(
        '$number',
        style: const TextStyle(
          color: Colors.white,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }
}

class _PlusSeparator extends StatelessWidget {
  const _PlusSeparator();

  @override
  Widget build(BuildContext context) {
    return const SizedBox(
      width: 20,
      height: 36,
      child: Center(child: Text('+', style: TextStyle(fontSize: 18))),
    );
  }
}
