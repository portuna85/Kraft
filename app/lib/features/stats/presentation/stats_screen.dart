import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import '../../../core/api/api_client.dart';
import '../../../core/error/app_exception.dart';
import '../domain/analysis_result.dart';
import '../domain/companion_stats.dart';
import '../domain/frequency_stats.dart';
import '../domain/pattern_stats.dart';

part 'stats_screen.g.dart';

@riverpod
Future<List<BallFrequency>> frequencyStats(
    FrequencyStatsRef ref, int? period) async {
  final res =
      await ref.watch(kraftApiClientProvider).getFrequency(period: period);
  return res.data ?? [];
}

@riverpod
Future<AnalysisResult> analysisResult(
    AnalysisResultRef ref, List<int> numbers) async {
  final res = await ref
      .watch(kraftApiClientProvider)
      .analyze({'numbers': numbers});
  return res.data!;
}

@riverpod
Future<PatternStats> patternStats(PatternStatsRef ref) async {
  final res = await ref.watch(kraftApiClientProvider).getPatterns();
  return res.data!;
}

@riverpod
Future<List<CompanionEntry>> companionStats(
    CompanionStatsRef ref, int target) async {
  final res =
      await ref.watch(kraftApiClientProvider).getCompanion(target);
  return res.data ?? [];
}

class StatsScreen extends StatelessWidget {
  const StatsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 4,
      child: Scaffold(
        appBar: AppBar(
          title: const Text('통계'),
          bottom: const TabBar(
            tabs: [
              Tab(text: '번호 빈도'),
              Tab(text: '패턴'),
              Tab(text: '동반 번호'),
              Tab(text: '분석'),
            ],
          ),
        ),
        body: const TabBarView(
          children: [
            _FrequencyTab(),
            _PatternTab(),
            _CompanionTab(),
            _AnalysisTab(),
          ],
        ),
      ),
    );
  }
}

// ── 번호 빈도 탭 ────────────────────────────────────────────────────────────

class _FrequencyTab extends ConsumerStatefulWidget {
  const _FrequencyTab();

  @override
  ConsumerState<_FrequencyTab> createState() => _FrequencyTabState();
}

class _FrequencyTabState extends ConsumerState<_FrequencyTab> {
  int? _period; // null = 전체

  static const _periods = [
    (label: '전체', value: null),
    (label: '최근 50회', value: 50),
    (label: '최근 100회', value: 100),
  ];

  @override
  Widget build(BuildContext context) {
    final async = ref.watch(frequencyStatsProvider(_period));
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          child: SegmentedButton<int?>(
            segments: _periods
                .map((p) =>
                    ButtonSegment(value: p.value, label: Text(p.label)))
                .toList(),
            selected: {_period},
            onSelectionChanged: (s) => setState(() => _period = s.first),
            style: const ButtonStyle(
              visualDensity: VisualDensity.compact,
            ),
          ),
        ),
        Expanded(
          child: async.when(
            loading: () => const Center(child: CircularProgressIndicator()),
            error: (e, _) => Center(child: Text(errorMessage(e))),
            data: (frequencies) {
              if (frequencies.isEmpty) {
                return const Center(child: Text('데이터가 없습니다'));
              }
              final maxCount = frequencies
                  .map((e) => e.count)
                  .reduce((a, b) => a > b ? a : b);
              return ListView.separated(
                padding: const EdgeInsets.all(8),
                itemCount: frequencies.length,
                separatorBuilder: (_, __) => const Divider(height: 1),
                itemBuilder: (_, i) {
                  final f = frequencies[i];
                  return _FrequencyRow(
                    frequency: f,
                    barRatio: f.count / maxCount,
                  );
                },
              );
            },
          ),
        ),
      ],
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

// ── 패턴 탭 ─────────────────────────────────────────────────────────────────

class _PatternTab extends ConsumerWidget {
  const _PatternTab();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(patternStatsProvider);
    return async.when(
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (e, _) => Center(child: Text(errorMessage(e))),
      data: (stats) => ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _SectionHeader('홀짝 패턴 (총 ${stats.totalDraws}회)'),
          const SizedBox(height: 8),
          ...stats.oddEvenStats.map((s) => Card(
                child: ListTile(
                  title: Text('홀수 ${s.oddCount}개 / 짝수 ${s.evenCount}개'),
                  subtitle: LinearProgressIndicator(
                    value: s.percent / 100,
                    minHeight: 6,
                  ),
                  trailing: Text('${s.percent.toStringAsFixed(1)}%'),
                ),
              )),
          const SizedBox(height: 16),
          const _SectionHeader('합산 범위 패턴'),
          const SizedBox(height: 8),
          ...stats.sumRangeStats.map((s) => Card(
                child: ListTile(
                  title: Text('${s.rangeStart}~${s.rangeEnd}'),
                  subtitle: LinearProgressIndicator(
                    value: s.percent / 100,
                    minHeight: 6,
                  ),
                  trailing: Text('${s.percent.toStringAsFixed(1)}%'),
                ),
              )),
        ],
      ),
    );
  }
}

// ── 동반 번호 탭 ─────────────────────────────────────────────────────────────

class _CompanionTab extends ConsumerStatefulWidget {
  const _CompanionTab();

  @override
  ConsumerState<_CompanionTab> createState() => _CompanionTabState();
}

class _CompanionTabState extends ConsumerState<_CompanionTab> {
  int? _selectedNumber;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 14, 16, 4),
          child: Text(
            '번호를 선택하면 함께 자주 나온 번호를 보여줍니다',
            style: Theme.of(context)
                .textTheme
                .bodySmall
                ?.copyWith(color: Colors.grey),
          ),
        ),
        _BallGrid(
          selected: _selectedNumber,
          onTap: (n) => setState(
              () => _selectedNumber = _selectedNumber == n ? null : n),
        ),
        const Divider(height: 1),
        Expanded(
          child: _selectedNumber == null
              ? const Center(
                  child: Text('위에서 번호를 선택하세요',
                      style: TextStyle(color: Colors.grey)))
              : _CompanionList(target: _selectedNumber!),
        ),
      ],
    );
  }
}

class _BallGrid extends StatelessWidget {
  const _BallGrid({
    required this.onTap,
    this.selected,
    this.selectedSet,
  });
  final int? selected;
  final Set<int>? selectedSet;
  final ValueChanged<int> onTap;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      child: Wrap(
        spacing: 6,
        runSpacing: 6,
        children: List.generate(45, (i) {
          final n = i + 1;
          final isSelected = selectedSet != null
              ? selectedSet!.contains(n)
              : selected == n;
          return GestureDetector(
            onTap: () => onTap(n),
            child: AnimatedContainer(
              duration: const Duration(milliseconds: 150),
              width: 36,
              height: 36,
              decoration: BoxDecoration(
                color: isSelected
                    ? _ballColor(n)
                    : _ballColor(n).withValues(alpha: 0.35),
                shape: BoxShape.circle,
                border: isSelected
                    ? Border.all(color: Colors.black54, width: 2)
                    : null,
              ),
              alignment: Alignment.center,
              child: Text(
                '$n',
                style: TextStyle(
                  color: isSelected ? Colors.white : Colors.black54,
                  fontSize: 11,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
          );
        }),
      ),
    );
  }

  Color _ballColor(int n) {
    if (n <= 10) return const Color(0xFFF9A825);
    if (n <= 20) return const Color(0xFF42A5F5);
    if (n <= 30) return const Color(0xFFEF5350);
    if (n <= 40) return const Color(0xFF757575);
    return const Color(0xFF66BB6A);
  }
}

class _CompanionList extends ConsumerWidget {
  const _CompanionList({required this.target});
  final int target;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(companionStatsProvider(target));
    return async.when(
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (e, _) => Center(child: Text(errorMessage(e))),
      data: (entries) {
        if (entries.isEmpty) {
          return const Center(child: Text('데이터가 없습니다'));
        }
        return ListView.separated(
          padding: const EdgeInsets.symmetric(vertical: 8),
          itemCount: entries.length,
          separatorBuilder: (_, __) => const Divider(height: 1),
          itemBuilder: (_, i) {
            final e = entries[i];
            return ListTile(
              leading: _SmallBall(e.number),
              title: Text('${e.count}회 함께 출현'),
              trailing: Text(
                '${e.percent.toStringAsFixed(1)}%',
                style: const TextStyle(fontWeight: FontWeight.w600),
              ),
            );
          },
        );
      },
    );
  }
}

class _SmallBall extends StatelessWidget {
  const _SmallBall(this.number);
  final int number;

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
      width: 36,
      height: 36,
      decoration: BoxDecoration(color: _color, shape: BoxShape.circle),
      alignment: Alignment.center,
      child: Text(
        '$number',
        style: const TextStyle(
          color: Colors.white,
          fontSize: 13,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }
}

// ── 분석 탭 ─────────────────────────────────────────────────────────────────

class _AnalysisTab extends ConsumerStatefulWidget {
  const _AnalysisTab();

  @override
  ConsumerState<_AnalysisTab> createState() => _AnalysisTabState();
}

class _AnalysisTabState extends ConsumerState<_AnalysisTab> {
  final Set<int> _selected = {};

  void _toggle(int n) {
    setState(() {
      if (_selected.contains(n)) {
        _selected.remove(n);
      } else if (_selected.length < 6) {
        _selected.add(n);
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final ready = _selected.length == 6;
    final numbers = [..._selected]..sort();
    final async =
        ready ? ref.watch(analysisResultProvider(numbers)) : null;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 14, 16, 4),
          child: Text(
            '번호 6개를 선택하면 과거 당첨 이력을 조회합니다',
            style: Theme.of(context)
                .textTheme
                .bodySmall
                ?.copyWith(color: Colors.grey),
          ),
        ),
        _BallGrid(
          selectedSet: _selected,
          onTap: _toggle,
        ),
        if (_selected.isNotEmpty)
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
            child: Row(
              children: [
                Text(
                  '선택: ${numbers.join(', ')}',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
                const Spacer(),
                TextButton(
                  onPressed: () => setState(() => _selected.clear()),
                  child: const Text('초기화'),
                ),
              ],
            ),
          ),
        const Divider(height: 1),
        Expanded(
          child: !ready
              ? Center(
                  child: Text(
                    '${6 - _selected.length}개 더 선택하세요',
                    style: const TextStyle(color: Colors.grey),
                  ),
                )
              : async!.when(
                  loading: () =>
                      const Center(child: CircularProgressIndicator()),
                  error: (e, _) => Center(child: Text(errorMessage(e))),
                  data: (result) => _AnalysisResult(result: result),
                ),
        ),
      ],
    );
  }
}

class _AnalysisResult extends StatelessWidget {
  const _AnalysisResult({required this.result});
  final AnalysisResult result;

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                _StatChip(
                  label: '1등',
                  count: result.firstPrizeCount,
                  color: Colors.amber.shade700,
                ),
                _StatChip(
                  label: '2등',
                  count: result.secondPrizeCount,
                  color: Colors.blueGrey,
                ),
              ],
            ),
          ),
        ),
        if (result.firstPrizeHits.isNotEmpty) ...[
          const SizedBox(height: 12),
          const _SectionHeader('1등 당첨 회차'),
          const SizedBox(height: 6),
          ...result.firstPrizeHits.map((h) => _HitTile(hit: h)),
        ],
        if (result.secondPrizeHits.isNotEmpty) ...[
          const SizedBox(height: 12),
          const _SectionHeader('2등 당첨 회차'),
          const SizedBox(height: 6),
          ...result.secondPrizeHits.map((h) => _HitTile(hit: h)),
        ],
        if (result.firstPrizeCount == 0 && result.secondPrizeCount == 0)
          Padding(
            padding: const EdgeInsets.only(top: 24),
            child: Center(
              child: Text(
                '과거 당첨 이력이 없습니다',
                style:
                    TextStyle(color: Colors.grey.shade600),
              ),
            ),
          ),
      ],
    );
  }
}

class _StatChip extends StatelessWidget {
  const _StatChip(
      {required this.label, required this.count, required this.color});
  final String label;
  final int count;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Text(label,
            style: TextStyle(
                color: color, fontWeight: FontWeight.bold, fontSize: 13)),
        const SizedBox(height: 4),
        Text('$count회',
            style: TextStyle(
                fontSize: 22, fontWeight: FontWeight.bold, color: color)),
      ],
    );
  }
}

class _HitTile extends StatelessWidget {
  const _HitTile({required this.hit});
  final PrizeHit hit;

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: 4),
      child: ListTile(
        dense: true,
        title: Text('제 ${hit.round}회'),
        trailing: Text(hit.drawDate,
            style: const TextStyle(color: Colors.grey, fontSize: 12)),
      ),
    );
  }
}

// ── 공통 ─────────────────────────────────────────────────────────────────────

class _SectionHeader extends StatelessWidget {
  const _SectionHeader(this.title);
  final String title;

  @override
  Widget build(BuildContext context) {
    return Text(title, style: Theme.of(context).textTheme.titleSmall);
  }
}
