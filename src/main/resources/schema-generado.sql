
    create table ai_response (
        type tinyint not null check ((type between 0 and 1)),
        created_date datetime(6) not null,
        id bigint not null auto_increment,
        user_id bigint not null,
        payload TEXT not null,
        primary key (id)
    ) engine=InnoDB;

    create table notification (
        is_read bit not null,
        creation_date datetime(6) not null,
        id bigint not null auto_increment,
        user_id bigint not null,
        message varchar(255) not null,
        title varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table progress_record (
        completed_tasks int default 0 not null,
        date date not null,
        total_tasks int default 0 not null,
        id bigint not null auto_increment,
        user_id bigint not null,
        primary key (id)
    ) engine=InnoDB;

    create table recurring_task (
        active bit not null,
        created_date datetime(6) not null,
        id bigint not null auto_increment,
        latest_generated_date datetime(6),
        subject_id bigint not null,
        description varchar(255),
        frequency varchar(255) not null,
        title varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table subject (
        archived bit not null,
        id bigint not null auto_increment,
        user_id bigint not null,
        description varchar(255),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table task (
        assigned_date datetime(6) not null,
        completed_date datetime(6),
        deadline datetime(6),
        id bigint not null auto_increment,
        recurring_task_id bigint,
        subject_id bigint not null,
        description varchar(255),
        title varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table user (
        creation_date datetime(6) not null,
        id bigint not null auto_increment,
        email varchar(255) not null,
        password varchar(255) not null,
        username varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table user 
       add constraint UKob8kqyqqgmefl0aco34akdtpe unique (email);

    alter table user 
       add constraint UKsb8bbouer5wak8vyiiy4pf2bx unique (username);

    alter table ai_response 
       add constraint FK4cybfaaxeysyx6rvyauuluyaf 
       foreign key (user_id) 
       references user (id);

    alter table notification 
       add constraint FKb0yvoep4h4k92ipon31wmdf7e 
       foreign key (user_id) 
       references user (id);

    alter table progress_record 
       add constraint FKp58nkvi419fghq4ggq6io0joa 
       foreign key (user_id) 
       references user (id);

    alter table recurring_task 
       add constraint FKss246b6mvaxbych7ra6g7qirw 
       foreign key (subject_id) 
       references subject (id);

    alter table subject 
       add constraint FK96a455takm74cb3lol4571d7a 
       foreign key (user_id) 
       references user (id);

    alter table task 
       add constraint FKdc3th1qv1iv867ilkmye4vray 
       foreign key (recurring_task_id) 
       references recurring_task (id);

    alter table task 
       add constraint FK5k22wv8pvap89p7wpo0ghs95g 
       foreign key (subject_id) 
       references subject (id);

    create table ai_response (
        type tinyint not null,
        created_date datetime(6) not null,
        id bigint not null auto_increment,
        user_id bigint not null,
        payload TEXT not null,
        primary key (id)
    ) engine=InnoDB;

    create table notification (
        is_read bit not null,
        creation_date datetime(6) not null,
        id bigint not null auto_increment,
        user_id bigint not null,
        message varchar(255) not null,
        title varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table progress_record (
        completed_tasks int default 0 not null,
        date date not null,
        total_tasks int default 0 not null,
        id bigint not null auto_increment,
        user_id bigint not null,
        primary key (id)
    ) engine=InnoDB;

    create table recurring_task (
        active bit not null,
        created_date datetime(6) not null,
        id bigint not null auto_increment,
        latest_generated_date datetime(6),
        subject_id bigint not null,
        description varchar(255),
        frequency varchar(255) not null,
        title varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table subject (
        archived bit not null,
        id bigint not null auto_increment,
        user_id bigint not null,
        description varchar(255),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table task (
        assigned_date datetime(6) not null,
        completed_date datetime(6),
        deadline datetime(6),
        id bigint not null auto_increment,
        recurring_task_id bigint,
        subject_id bigint not null,
        description varchar(255),
        title varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table user (
        creation_date datetime(6) not null,
        id bigint not null auto_increment,
        email varchar(255) not null,
        password varchar(255) not null,
        username varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table user 
       add constraint UKob8kqyqqgmefl0aco34akdtpe unique (email);

    alter table user 
       add constraint UKsb8bbouer5wak8vyiiy4pf2bx unique (username);

    alter table ai_response 
       add constraint FK4cybfaaxeysyx6rvyauuluyaf 
       foreign key (user_id) 
       references user (id);

    alter table notification 
       add constraint FKb0yvoep4h4k92ipon31wmdf7e 
       foreign key (user_id) 
       references user (id);

    alter table progress_record 
       add constraint FKp58nkvi419fghq4ggq6io0joa 
       foreign key (user_id) 
       references user (id);

    alter table recurring_task 
       add constraint FKss246b6mvaxbych7ra6g7qirw 
       foreign key (subject_id) 
       references subject (id);

    alter table subject 
       add constraint FK96a455takm74cb3lol4571d7a 
       foreign key (user_id) 
       references user (id);

    alter table task 
       add constraint FKdc3th1qv1iv867ilkmye4vray 
       foreign key (recurring_task_id) 
       references recurring_task (id);

    alter table task 
       add constraint FK5k22wv8pvap89p7wpo0ghs95g 
       foreign key (subject_id) 
       references subject (id);

    create table ai_response (
        type tinyint not null check ((type between 0 and 1)),
        created_date datetime(6) not null,
        id bigint not null auto_increment,
        user_id bigint not null,
        payload TEXT not null,
        primary key (id)
    ) engine=InnoDB;

    create table notification (
        is_read bit not null,
        creation_date datetime(6) not null,
        id bigint not null auto_increment,
        user_id bigint not null,
        message varchar(255) not null,
        title varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table progress_record (
        completed_tasks int default 0 not null,
        date date not null,
        total_tasks int default 0 not null,
        id bigint not null auto_increment,
        user_id bigint not null,
        primary key (id)
    ) engine=InnoDB;

    create table recurring_task (
        active bit not null,
        created_date datetime(6) not null,
        id bigint not null auto_increment,
        latest_generated_date datetime(6),
        subject_id bigint not null,
        description varchar(255),
        frequency varchar(255) not null,
        title varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table subject (
        archived bit not null,
        id bigint not null auto_increment,
        user_id bigint not null,
        description varchar(255),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table task (
        assigned_date datetime(6) not null,
        completed_date datetime(6),
        deadline datetime(6),
        id bigint not null auto_increment,
        recurring_task_id bigint,
        subject_id bigint not null,
        description varchar(255),
        title varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table user (
        creation_date datetime(6) not null,
        id bigint not null auto_increment,
        email varchar(255) not null,
        password varchar(255) not null,
        username varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    alter table user 
       add constraint UKob8kqyqqgmefl0aco34akdtpe unique (email);

    alter table user 
       add constraint UKsb8bbouer5wak8vyiiy4pf2bx unique (username);

    alter table ai_response 
       add constraint FK4cybfaaxeysyx6rvyauuluyaf 
       foreign key (user_id) 
       references user (id);

    alter table notification 
       add constraint FKb0yvoep4h4k92ipon31wmdf7e 
       foreign key (user_id) 
       references user (id);

    alter table progress_record 
       add constraint FKp58nkvi419fghq4ggq6io0joa 
       foreign key (user_id) 
       references user (id);

    alter table recurring_task 
       add constraint FKss246b6mvaxbych7ra6g7qirw 
       foreign key (subject_id) 
       references subject (id);

    alter table subject 
       add constraint FK96a455takm74cb3lol4571d7a 
       foreign key (user_id) 
       references user (id);

    alter table task 
       add constraint FKdc3th1qv1iv867ilkmye4vray 
       foreign key (recurring_task_id) 
       references recurring_task (id);

    alter table task 
       add constraint FK5k22wv8pvap89p7wpo0ghs95g 
       foreign key (subject_id) 
       references subject (id);
