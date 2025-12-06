-- Test for NPE when a foreign key references an entity that exists in XML but not in database
-- This creates a scenario where join reverse engineering might fail

create table publisher
(
    publisher_id int identity not null,
    name varchar(200) not null,
    primary key (publisher_id)
);

create table book
(
    book_id int identity not null,
    title varchar(200) not null,
    publisher_id int not null,
    primary key (book_id),
    foreign key (publisher_id) references publisher (publisher_id)
);

insert into publisher values (1, 'Test Publisher');
insert into book values (1, 'Test Book', 1);
