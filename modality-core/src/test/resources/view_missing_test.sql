-- Test for NPE when a view entity is declared with explicit PK but the view doesn't exist

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
