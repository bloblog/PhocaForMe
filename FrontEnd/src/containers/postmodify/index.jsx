import React, { useState, useEffect, useRef } from "react";
import { useParams, useNavigate, useLocation } from "react-router-dom";
import { Container, Button, CircularProgress } from "@mui/material";
import BarterModify from "./option.jsx";
import TypeDropdown from "@/components/Dropdown/type.jsx";
import BasicModal from "@/components/Modal/index.jsx";
import ImageInput from "@/components/Input/image.jsx";

import { getPost, modifyPost } from "@/api/post.jsx";
import PairButton from "@/components/Button/pair.jsx";
import makeFormData from "../../utils/makeFormData.jsx";
import AmazonSrc from "../../constants/amazonS3.jsx";
import isComplete from "../../utils/isComplete.jsx";
import { useSelector } from "react-redux";

const PostModify = () => {
  const { id } = useParams();

  const [images, setImages] = useState([]);
  const [imagePreviews, setImagePreviews] = useState([]);
  const [title, setTitle] = useState("");
  const [groupId, setGroupId] = useState(0);
  const [ownIdolMembers, setOwnIdolMembers] = useState([]);
  const [findIdolMembers, setFindIdolMembers] = useState([]);
  const [cardType, setCardType] = useState("");
  const [content, setContent] = useState("");

  const [dataFetching, setDataFetching] = useState(true);
  const [loading, setLoading] = useState(false);
  const [imagesChanged, setImagesChanged] = useState(false); // 이미지 변경 여부 추적
  const [open, setOpen] = useState(false);

  const imageContainerRef = useRef(null);

  const user = useSelector((state) => state.user.user);
  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    if (images.length > 0) {
      const defaultImagePreviews = images.map((photo) => AmazonSrc + photo);
      setImagePreviews(defaultImagePreviews);
    }
  }, [images]);

  useEffect(() => {
    // 내가 작성한 글이 아닌 경우
    if (user != location.state) {
      navigate("/post");
    }
    getPost(
      id,
      (data) => {
        setTitle(data.data.title);
        setImages(data.data.photos);
        setGroupId(data.data.groupId);
        setOwnIdolMembers(data.data.ownIdolMembers);
        setFindIdolMembers(data.data.findIdolMembers);
        setCardType(data.data.cardType);
        setContent(data.data.content);
      },
      (err) => {
        console.error("Error Get Post: " + err);
      }
    );
    setDataFetching(false);
  }, []);

  useEffect(() => {
    if (imageContainerRef.current) {
      imageContainerRef.current.scrollLeft =
        imageContainerRef.current.scrollWidth;
    }
  }, [imagePreviews]);

  const handleImageFormat = async (images) => {
    const formattedImage = [];

    const promises = images.map(async (filePath) => {
      if (typeof filePath == "object") {
        return filePath;
      } else {
        const response = await fetch(AmazonSrc + filePath);
        const blob = await response.blob();
        const type = filePath.split(".").pop();
        const file = new File([blob], filePath.split("/")[1].substring(36), {
          type: `image/${type}`,
        });
        return file;
      }
    });

    const results = await Promise.all(promises);
    formattedImage.push(...results);

    return formattedImage;
  };

  const handleOwnMemberSelection = (members) => {
    setOwnIdolMembers(members);
  };

  const handleTargetMemberSelection = (members) => {
    setFindIdolMembers(members);
  };

  const handleTypeChange = (cardType) => {
    setCardType(cardType);
  };

  const handleTitleChange = (event) => {
    setTitle(event.target.value);
  };

  const handleContentChange = (event) => {
    setContent(event.target.value);
  };

  const handleImageDelete = (index) => {
    setImages((prevImages) => {
      const newImages = [...prevImages];
      newImages.splice(index, 1);
      return newImages;
    });

    setImagePreviews((prevImagePreviews) => {
      const newImagePreviews = [...prevImagePreviews];
      newImagePreviews.splice(index, 1);
      return newImagePreviews;
    });

    setImagesChanged(true);
  };

  const handleImageChange = async (event) => {
    const files = event.target.files;
    const newImages = Array.from(files);

    const newImagePreviews = await Promise.all(
      newImages.map((file) => {
        return new Promise((resolve) => {
          const reader = new FileReader();
          reader.onloadend = () => {
            resolve(reader.result);
          };
          reader.readAsDataURL(file);
        });
      })
    );

    // 빈 프리뷰 필터링
    const filteredNewImagePreviews = newImagePreviews.filter(
      (preview) => preview
    );

    // 이미지 미리보기 업데이트
    setImagePreviews((prevImagePreviews) => [
      ...prevImagePreviews,
      ...filteredNewImagePreviews,
    ]);

    // 이미지 상태 업데이트
    setImages((prevImages) => {
      return [...prevImages, ...newImages];
    });
  };

  const handleModifyClick = () => {
    handleImageFormat(images).then((formattedIamge) => {
      const formData = makeFormData(
        formattedIamge,
        title,
        groupId,
        ownIdolMembers,
        findIdolMembers,
        cardType,
        content
      );

      // 안 채워진 항목 쳐내기
      const state = isComplete(formData);
      if (state.length == 0) {
        setLoading(true);
        modifyPost(
          formData,
          id,
          (data) => {
            setLoading(false);
            navigate(`/post/${id}`);
          },
          (error) => {
            setLoading(false);
            console.error("Error modifying post:", error);
          }
        );
      } else {
        handleClickOpen();
        setLoading(false);
      }
    });
  };

  const handleCancelButton = () => {
    navigate("/post");
  };

  if (dataFetching) {
    return <CircularProgress />;
  }

  const handleClickOpen = () => {
    setOpen(true);
  };

  const handleClose = () => {
    setOpen(false);
  };

  return (
    <Container>
      <BasicModal
        handleClose={handleClose}
        open={open}
        content={"모든 항목은 필수 입력 사항입니다!"}
      />
      <h2 className="write-title">게시글 수정하기</h2>
      <div className={loading ? "write-container loading" : "write-container"}>
        <div id="image-input">
          <div id="image-list" ref={imageContainerRef}>
            {imagePreviews &&
              imagePreviews.map((preview, index) => (
                <div
                  className="img-preview"
                  key={index}
                  style={{ backgroundImage: `url(${preview})` }}
                  onClick={() => handleImageDelete(index)}
                ></div>
              ))}
            <ImageInput onChange={handleImageChange} />
          </div>
          <p className="info-msg">* 사진 클릭 시 삭제됩니다.</p>
        </div>
        <div id="title-container">
          <h3>제목</h3>
          <input
            id="title-input"
            value={title}
            onChange={handleTitleChange}
            variant="outlined"
            placeholder="앨범명, 버전명을 입력하세요"
          />
        </div>
        <div id="group-member-input">
          <BarterModify
            groupId={groupId}
            defaultOwnMember={ownIdolMembers}
            defaultTargetMember={findIdolMembers}
            onChange={(ownMembers, targetMembers) => {
              handleOwnMemberSelection(ownMembers);
              handleTargetMemberSelection(targetMembers);
            }}
          />
        </div>

        <div id="card-input">
          <h3>포토카드 종류</h3>
          <TypeDropdown
            defaultCardType={cardType}
            onChange={(type) => {
              handleTypeChange(type);
            }}
          />
        </div>

        <div id="content-input-container">
          <h3>상세 내용</h3>
          <textarea
            className="content-input"
            value={content}
            onChange={handleContentChange}
            placeholder="포토카드 상태에 대한 세부 내용을 적어주세요."
          />
        </div>
        <PairButton
          type1={"수정"}
          type2={"취소"}
          handler1={handleModifyClick}
          handler2={handleCancelButton}
        ></PairButton>
      </div>
      {loading && (
        <div className="loading-spinner">
          <CircularProgress />
        </div>
      )}
    </Container>
  );
};

export default PostModify;
