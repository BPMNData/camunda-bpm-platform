create database `pattern_eval_a`;
create database `pattern_eval_b`;
grant usage on *.* to bpmndata@localhost identified by 'bpmndata';
grant all privileges on pattern_eval_a.* to bpmndata@localhost ;
grant all privileges on pattern_eval_b.* to bpmndata@localhost ;