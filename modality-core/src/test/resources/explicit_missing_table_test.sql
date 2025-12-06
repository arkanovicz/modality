-- Test for NPE when an entity has an explicit table= attribute pointing to a non-existent table
-- and there's a foreign key referencing it

create table author
(
    author_id int identity not null,
    name varchar(200) not null,
    primary key (author_id)
);

insert into author values (1, 'Test Author');
