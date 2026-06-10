import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../../core/api/api_client.dart';
import '../domain/frequency_stats.dart';
import '../domain/pattern_stats.dart';

part 'stats_screen.g.dart';

@riverpod
Future<FrequencyStats> frequencyStats(FrequencyStatsRef ref) async {
  final res = await ref.watch(kraftApiClientProvider).getFrequency();
  return res.data!;
}

@riverpod
Future<PatternStats> patternStats(PatternStatsRef ref) async {
  final res = await ref.watch(kraftApiClientProvider).getPatterns();
  return res.data!;
}

class StatsScreen extends StatelessWidget {
  const StatsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 2,
      child: Scaffold(
        appBar: AppBar(
          title: const Text('통계'),
          bottom: const TabBar(
            tabs: [
              Tab(text: '번호 빈도'),
              Tab(text: '패턴'),
            ],
          ),
        ),
        body: const TabBarView(
          children: [
            _FrequencyTab(),
            _PatternTab(),
          ],
        ),
      ),
    );
  }
}

class _FrequencyTab extends ConsumerWidget {
  const _FrequencyTab();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(frequencyStatsProvider);
    return async.when(
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (e, _) => Center(child: Text(e.toString())),
      data: (stats) => ListView.separated(
        padding: const EdgeInsets.all(8),
        itemCount: stats.frequencies.length,
        separatorBuilder: (_, __) => const Divider(height: 1),
        itemBuilder: (_, i) {
          final f = stats.frequencies[i];
          final maxCount = stats.frequencies
              .map((e) => e.count)
              .reduce((a, b) => a > b ? a : b);
          return _FrequencyRow(
            frequency: f,
            barRatio: f.count / maxCount,
          );
        },
      ),
    );
  }
}

class _FrequencyRow extends StatelessWidget {
  const _FrequencyRow({required this.frequency, required this.barRatio});
  final BallFrequency frequency;
  final double barRatio;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6, horizontal: 12),
      child: Row(
        children: [
          SizedBox(
            width: 28,
            child: Text(
              '${frequency.number}',
              style: const TextStyle(fontWeight: FontWeight.bold),
              textAlign: TextAlign.right,
            ),
          ),
          const SizedBox(width: 8),
          Expanded(
            child: ClipRRect(
              borderRadius: BorderRadius.circular(4),
              child: LinearProgressIndicator(
                value: barRatio,
                minHeight: 18,
                backgroundColor: theme.colorScheme.surfaceContainerHighest,
                valueColor: AlwaysStoppedAnimation(theme.colorScheme.primary),
              ),
            ),
          ),
          const SizedBox(width: 8),
          Text('${frequency.count}회'),
        ],
      ),
    );
  }
}

class _PatternTab extends ConsumerWidget {
  const _PatternTab();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(patternStatsProvider);
    return async.when(
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (e, _) => Center(child: Text(e.toString())),
      data: (stats) => ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Card(
            child: Column(
              children: [
                ListTile(
                  title: const Text('평균 합계'),
                  trailing: Text(stats.avgSum.toStringAsFixed(1)),
                ),
                ListTile(
                  title: const Text('평균 홀수 개수'),
                  trailing: Text(stats.avgOddCount.toStringAsFixed(1)),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
