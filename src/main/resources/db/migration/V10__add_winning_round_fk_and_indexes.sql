create index idx_winning_numbers_version on winning_numbers (version);
create index idx_winning_numbers_fetched_at on winning_numbers (fetched_at);

alter table lotto_fetch_logs
    add column winning_round int null;

update lotto_fetch_logs
set winning_round = drw_no
where status = 'SUCCESS'
  and drw_no in (select round from winning_numbers);

alter table lotto_fetch_logs
    add constraint fk_lotto_fetch_logs_winning_round
        foreign key (winning_round)
            references winning_numbers (round)
            on delete set null;

create index idx_lotto_fetch_logs_winning_round on lotto_fetch_logs (winning_round);
