-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"). you may not use this file except in compliance
-- with the License.  You may obtain a copy of the License at
--
--   http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied.  See the License for the
-- specific language governing permissions and limitations
-- under the License.    

create table publisher
(
    publisher_id int not null,
    name varchar(200) not null,
    primary key (publisher_id)
);

create table author
(
    author_id int not null,
    name varchar(200) not null,
    primary key (author_id)
);

create table book
(
    book_id int not null,
    title varchar(200) not null,
    published date not null,
    publisher_id int not null,
    primary key (book_id),
    foreign key (publisher_id) references publisher (publisher_id)
);

create table book_author
(
    book_id int not null,
    author_id int not null,
    primary key (book_id, author_id),
    foreign key (book_id) references book (book_id),
    foreign key (author_id) references author (author_id)
);

create table user
(
    us_id int not null,
    login varchar(200) not null,
    password varchar(200) not null,
    primary key (us_id)
);

create table remember_me
(
     us_id INT NOT NULL,
     secure_key VARCHAR(20) NOT NULL,
     creation DATETIME NOT NULL,
     PRIMARY KEY (us_id, secure_key),
     FOREIGN KEY (us_id) REFERENCES user (us_id)
);

insert into publisher values (1, 'Green Penguin Books'), (2, 'Serious Stuff Publishing');
insert into book values (1, 'The Astonishing Life of Duncan Aston', '2018-05-09', 1), (2, 'Beyond the Surface of Things', '2018-06-20', 2), (3, 'Seeds and Flowers', '2005-06-01', 2);
insert into author values (1, 'Graham Brigovicz'), (2, 'Robert Willhelm'), (3, 'Vanessa Hopkins'), (4, 'Kai Mattey');
insert into book_author values (1, 1), (1, 2), (2, 3), (3, 4);
insert into user values (1, 'nestor', 'secret');
