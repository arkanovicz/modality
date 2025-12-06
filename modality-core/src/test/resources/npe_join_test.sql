-- Test for NPE at line 1032 in declareUpstreamJoin
-- Scenario: entity declared in XML with sqlPrimaryKey but table doesn't exist
-- The sqlPrimaryKey causes the entity to pass the null check in getJoins()
-- but getPrimaryKey() will be null because no columns exist

create table book
(
    book_id int identity not null,
    title varchar(200) not null,
    author_id int,
    primary key (book_id)
);

insert into book values (1, 'Test Book', 1);
