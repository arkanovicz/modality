-- Test for NPE when entity in XML model doesn't exist in database
-- Case 1: Simple missing entity (publisher is declared in XML but doesn't exist in DB)

create table author
(
    author_id int identity not null,
    name varchar(200) not null,
    primary key (author_id)
);

create table book
(
    book_id int identity not null,
    title varchar(200) not null,
    author_id int not null,
    primary key (book_id),
    foreign key (author_id) references author (author_id)
);

insert into author values (1, 'Test Author');
insert into book values (1, 'Test Book', 1);
