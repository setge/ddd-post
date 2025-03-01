package com.setge.dddpost.domain.post.application;

import static com.setge.dddpost.Fixtures.anComment;
import static com.setge.dddpost.Fixtures.anJoin;
import static com.setge.dddpost.Fixtures.anNestedComment;
import static com.setge.dddpost.Fixtures.anPost;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;
import com.setge.dddpost.domain.comment.application.CommentDto;
import com.setge.dddpost.domain.comment.application.CommentSearchDto;
import com.setge.dddpost.domain.comment.application.CommentService;
import com.setge.dddpost.domain.comment.domain.CommentRepository;
import com.setge.dddpost.domain.member.application.MemberDto.Join;
import com.setge.dddpost.domain.member.application.MemberService;
import com.setge.dddpost.domain.member.domain.MemberRepository;
import com.setge.dddpost.domain.nestedcomment.application.NestedCommentService;
import com.setge.dddpost.domain.nestedcomment.domain.NestedCommentRepository;
import com.setge.dddpost.domain.post.application.PostDto.ChangeRecommendPost;
import com.setge.dddpost.domain.post.application.PostDto.Create;
import com.setge.dddpost.domain.post.application.PostDto.DetailedSearchCondition;
import com.setge.dddpost.domain.post.application.PostDto.RecommendPost;
import com.setge.dddpost.domain.post.application.PostDto.Response;
import com.setge.dddpost.domain.post.application.PostDto.Update;
import com.setge.dddpost.domain.post.domain.Post.PostType;
import com.setge.dddpost.domain.post.domain.PostRepository;
import java.util.List;
import java.util.stream.IntStream;
import javax.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@SpringBootTest
class PostServiceImplTest {

  @Autowired
  private PostService postService;

  @Autowired
  private PostRepository postRepository;

  @Autowired
  private MemberService memberService;

  @Autowired
  private MemberRepository memberRepository;

  @Autowired
  private CommentService commentService;

  @Autowired
  private NestedCommentService nestedCommentService;

  @Autowired
  protected EntityManager em;


  @AfterEach
  void tearDown() {
    System.out.println("delete All");
    postRepository.deleteAll();
    memberRepository.deleteAll();
  }


  @Test
  @DisplayName("게시물에 달린 댓글 + 대댓글 조회")
  public void getPostAndCommentAndNestedComment() {

    // given
    Join join = anJoin()
        .nickname("펭귄")
        .build();
    Long joinId = memberService.joinMember(join).getId();

    Create create = anPost()
        .userId(joinId)
        .postImages(getCreatePostImages())
        .build();

    Long postId = postService.createPost(create).getId();

    IntStream.rangeClosed(0, 3).forEach(i -> {
      Long commentId = commentService.createComment(postId, anComment(joinId).build()).getId();

      IntStream.rangeClosed(0, 5).forEach(j -> {
        nestedCommentService.createNestedComment(commentId, anNestedComment(joinId).build());
      });

    });

    // when
    Response post = postService.getPost(postId);

    // then
    assertThat(post.getComments().size()).isEqualTo(4);
    for (CommentDto.Response comment : post.getComments()) {
      assertThat(comment.getNestedComments().size()).isEqualTo(6);
    }

  }


  @Test
  @DisplayName("게시물 등록")
  void createPost() {

    // given
    Join join = anJoin()
        .nickname("펭귄")
        .build();
    Long joinId = memberService.joinMember(join).getId();

    Create create = anPost()
        .userId(joinId)
        .postImages(getCreatePostImages())
        .build();

    // when
    Long id = postService.createPost(create).getId();

    System.out.println("flush And Clear");
    flushAndClear();

    Response post = postService.getPost(id);

    // then
    assertThat(post.getType()).isEqualTo(create.getType().toString());
    assertThat(post.getUserId()).isEqualTo(create.getUserId());
    assertThat(post.getNickname()).isEqualTo(join.getNickname());
    assertThat(post.getTitle()).isEqualTo(create.getTitle());
    assertThat(post.getContent()).isEqualTo(create.getContent());
    assertThat(post.getPostImages().size()).isEqualTo(create.getPostImages().size());

  }

  public void flushAndClear() {
    em.flush();
    em.clear();
  }


  @Test
  @DisplayName("게시물 조회")
  void getPost() {

    // given
    Join join = anJoin()
        .nickname("펭귄")
        .build();
    Long joinId = memberService.joinMember(join).getId();

    Create create = anPost()
        .userId(joinId)
        .postImages(getCreatePostImages())
        .build();

    // whenÎ
    Long postId = postService.createPost(create).getId();
    Response post = postService.getPost(postId);

    // then
    assertThat(post.getId()).isEqualTo(postId);
    assertThat(post.getType()).isEqualTo(create.getType().toString());
    assertThat(post.getUserId()).isEqualTo(joinId);
    assertThat(post.getNickname()).isEqualTo(join.getNickname());
    assertThat(post.getTitle()).isEqualTo(create.getTitle());
    assertThat(post.getContent()).isEqualTo(create.getContent());
    assertThat(post.getPostImages().size()).isEqualTo(create.getPostImages().size());

  }

  @Test
  @DisplayName("게시물 수정")
  void updatePost() {

    // given
    Join join = anJoin()
        .nickname("펭귄")
        .build();
    Long joinId = memberService.joinMember(join).getId();

    Create create = anPost()
        .userId(joinId)
        .postImages(getCreatePostImages())
        .build();
    Long postId = postService.createPost(create).getId();

    // when
    PostDto.Update update = Update.builder()
        .type(PostType.HORROR)
        .title("수정 제목")
        .content("수정 내용")
        .postImages(
            Lists.newArrayList(PostImageDto.Create.builder()
                .imageUrl("modi.jpg")
                .priority(11)
                .build()))
        .build();

    Response updatePost = postService.updatePost(postId, update);

    // then
    assertThat(updatePost.getId()).isEqualTo(postId);
    assertThat(updatePost.getType()).isEqualTo(update.getType().toString());
    assertThat(updatePost.getTitle()).isEqualTo(update.getTitle());
    assertThat(updatePost.getContent()).isEqualTo(update.getContent());
    assertThat(updatePost.getPostImages().size()).isEqualTo(update.getPostImages().size());

  }

  @Test
  @DisplayName("게시물 삭제")
  void deletePost() {

    // given
    Join join = anJoin()
        .nickname("펭귄")
        .build();
    Long joinId = memberService.joinMember(join).getId();

    Create create = anPost()
        .userId(joinId)
        .postImages(getCreatePostImages())
        .build();
    Long postId = postService.createPost(create).getId();

    // when
    postService.deletePost(postId);

    // then
    assertThat(postRepository.findById(postId)).isEmpty();

  }

  @Test
  @DisplayName("추천 게시물 선정 or 변경")
  void changeRecommendPost() {

    // given

    Join join = anJoin()
        .nickname("펭귄")
        .build();
    Long joinId = memberService.joinMember(join).getId();

    List<RecommendPost> recommendPosts = Lists.newArrayList();

    IntStream.rangeClosed(1, 5).forEach(i -> {
      Create create = Create.builder()
          .type(PostType.FUNNY)
          .userId(joinId)
          .title("재미있는 자료 올립니다 ㅋㅋㅋ")
          .content("6월 13일 있었던 일 ㅋㅋㅋ")
          .postImages(
              Lists.newArrayList(PostImageDto.Create.builder()
                  .imageUrl("image " + i)
                  .priority(i)
                  .build()))
          .build();

      Long id = postService.createPost(create).getId();

      if (i % 2 == 0) {
        recommendPosts.add(RecommendPost.builder().id(id).recommend(true).build());
      }
    });

    // when
    ChangeRecommendPost changeRecommendPost = ChangeRecommendPost.builder()
        .recommendPosts(recommendPosts)
        .build();

    postService.changeRecommendPost(changeRecommendPost);

    // then
    for (RecommendPost result : recommendPosts) {
      assertThat(result.getRecommend()).isTrue();
    }

  }

  @Transactional
  @Test
  @DisplayName("게시물 검색")
  void retrievePost() {

    // given
    Join join = anJoin()
        .nickname("펭귄")
        .build();
    Long joinId1 = memberService.joinMember(join).getId();

    join = anJoin()
        .email("sample@naver.com")
        .nickname("참새")
        .build();
    Long joinId2 = memberService.joinMember(join).getId();

    List<PostImageDto.Create> createImages = getCreatePostImages();

    IntStream.rangeClosed(1, 31).forEach(i -> {

      Create create;

      if (i % 2 == 0) {
        create = Create.builder()
            .type(PostType.FUNNY)
            .userId(joinId1)
            .title("웃긴 자료 " + i)
            .content("재밌었던 일 " + i + "번째 !")
            .postImages(createImages)
            .build();
      } else {
        create = Create.builder()
            .type(PostType.FUNNY)
            .userId(joinId2)
            .title("웃긴 자료 " + i)
            .content("재밌었던 일 " + i + "번째 !")
//            .postImages(createImages)
            .build();
      }
      postService.createPost(create);
    });

    // when

    System.out.println("flush And Clear");
    flushAndClear();

    DetailedSearchCondition searchCondition = DetailedSearchCondition.builder()
        .searchOption("nickname")
        .keyword("참새")
        .build();

    Page<Response> responses = postService.retrievePost(searchCondition, PageRequest.of(0, 10));

    // then
    assertThat(responses.getTotalElements()).isEqualTo(16);

  }


  private List<PostImageDto.Create> getCreatePostImages() {

    List<PostImageDto.Create> createImages = Lists.newArrayList();

    IntStream.rangeClosed(1, 11).parallel().forEach(i -> {
      PostImageDto.Create create = PostImageDto.Create.builder()
          .imageUrl("url " + i)
          .priority(i)
          .build();
      createImages.add(create);
    });

    return createImages;
  }

  private List<PostImageDto.Create> getCreateImage() {

    PostImageDto.Create createImage1 = PostImageDto.Create.builder()
        .imageUrl("https://exam-bucket.s3.ap-northeast-2.amazonaws.com/data/image_1315_100.jpg")
        .priority(1)
        .build();

    PostImageDto.Create createImage2 = PostImageDto.Create.builder()
        .imageUrl("https://exam-bucket.s3.ap-northeast-2.amazonaws.com/data/image_1315_300.jpg")
        .priority(3)
        .build();

    PostImageDto.Create createImage3 = PostImageDto.Create.builder()
        .imageUrl("https://exam-bucket.s3.ap-northeast-2.amazonaws.com/data/image_1315_200.jpg")
        .priority(2)
        .build();

    PostImageDto.Create createImage4 = PostImageDto.Create.builder()
        .imageUrl("https://exam-bucket.s3.ap-northeast-2.amazonaws.com/data/image_1315_400.jpg")
        .priority(4)
        .build();

    List<PostImageDto.Create> createList = Lists.newArrayList();
    createList.add(createImage1);
    createList.add(createImage2);
    createList.add(createImage3);
    createList.add(createImage4);

    return createList;
  }

}